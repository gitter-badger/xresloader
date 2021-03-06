package com.owent.xresloader.data.src;

import com.owent.xresloader.ProgramOptions;
import com.owent.xresloader.data.err.ConvException;
import com.owent.xresloader.engine.ExcelEngine;
import com.owent.xresloader.engine.IdentifyEngine;
import com.owent.xresloader.scheme.SchemeConf;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by owentou on 2014/10/9.
 */
public class DataSrcExcel extends DataSrcImpl {

    private class DataSheetInfo {
        public Sheet table = null;
        public FormulaEvaluator formula = null;
        public Row current_row = null;
        public int next_index = 0;
        public int last_row_number = 0;
    }

    private HashMap<String, String> macros = null;
    private HashMap<String, Integer> nameMap = null;
    private LinkedList<DataSheetInfo> tables = new LinkedList<DataSheetInfo>();
    DataSheetInfo current = null;
    int recordNumber = 0;

    public DataSrcExcel() {
        super();

        macros = null;
        nameMap = new HashMap<String, Integer>();
    }

    @Override
    public int init() {
        int ret = init_macros();
        if (ret < 0)
            return ret;

        return init_sheet();
    }

    /***
     * macro表cache
     */
    static private class MacroCacheInfo {
        /*** file_path:key->value ***/
        private HashMap<String, HashMap<String, String> > cache = new HashMap<String, HashMap<String, String> >();
        private HashMap<String, String> empty = new HashMap<String, String>(); // 空项特殊处理
    };
    static private MacroCacheInfo macro_cache = new MacroCacheInfo();

    /***
     * 构建macro表cache，由于macro表大多数情况下都一样，所以
     */
    static HashMap<String, String> init_macro_with_cache(List<SchemeConf.DataInfo> src_list) {
        LinkedList<HashMap<String, String> > data_filled = new LinkedList<HashMap<String, String> >();

        // 枚举所有macro表信息
        for(SchemeConf.DataInfo src: src_list) {
            String file_path = "";
            if (false == src.file_path.isEmpty()) {
                file_path = src.file_path;
            }
            String fp_name = file_path + "/" + src.table_name;

            // 优先读缓存
            HashMap<String, String> res = macro_cache.cache.getOrDefault(fp_name, null);
            if (null != res) {
                data_filled.add(res);
                continue;
            }
            res = new HashMap<String, String>();

            if (file_path.isEmpty() || src.table_name.isEmpty() || src.data_col <= 0 || src.data_row <= 0) {
                System.err.println(
                    String.format("[WARNING] macro source \"%s\" (%s:%d，%d) ignored.", src.file_path, src.table_name, src.data_row, src.data_col)
                );
                continue;
            }

            Sheet tb = ExcelEngine.openSheet(file_path, src.table_name);
            if (null == tb) {
                System.err.println(
                    String.format("[WARNING] open macro source \"%s\" or table %s failed.", src.file_path, src.table_name)
                );
                continue;
            }

            FormulaEvaluator evalor = tb.getWorkbook().getCreationHelper().createFormulaEvaluator();

            int row_num = tb.getLastRowNum() + 1;
            for (int i = src.data_row - 1; i < row_num; ++i) {
                Row row = tb.getRow(i);
                DataContainer<String> key = ExcelEngine.cell2s(row, src.data_col - 1);
                DataContainer<String> val = ExcelEngine.cell2s(row, src.data_col, evalor);
                if (key.valid && val.valid && !key.get().isEmpty() && !val.get().isEmpty()) {
                    if (res.containsKey(key)) {
                        System.err.println(
                            String.format("[WARNING] macro key \"%s\" is used more than once.", key)
                        );
                    }
                    res.put(key.get(), val.get());
                }
            }

            macro_cache.cache.put(fp_name, res);
            data_filled.add(res);
        }

        // 空对象特殊处理
        if (data_filled.isEmpty()) {
            return macro_cache.empty;
        }

        // 只有一个macro项，则直接返回
        if (1 == data_filled.size()) {
            return data_filled.getFirst();
        }

        HashMap<String, String> ret = new HashMap<String, String>();
        for(HashMap<String, String> copy_from: data_filled) {
            ret.putAll(copy_from);
        }

        return ret;
    }

    /**
     * 初始化macros提花规则，先全部转为字符串，有需要后续在使用的时候再转
     *
     * @return
     */
    private int init_macros() {
        SchemeConf scfg = SchemeConf.getInstance();
        macros = init_macro_with_cache(scfg.getMacroSource());

        return 0;
    }

    private int init_sheet() {
        tables.clear();
        recordNumber = 0;
        nameMap.clear();

        SchemeConf scfg = SchemeConf.getInstance();
        String file_path = "";

        // 枚举所有数据表信息
        for(SchemeConf.DataInfo src: scfg.getDataSource()) {
            if (false == src.file_path.isEmpty()) {
                file_path = src.file_path;
            }

            if (file_path.isEmpty() || src.table_name.isEmpty() || src.data_col <= 0 || src.data_row <= 0) {
                System.err.println(
                    String.format("[ERROR] data source \"%s\" (%s:%d，%d) ignored.", src.file_path, src.table_name, src.data_row, src.data_col)
                );
                continue;
            }

            Sheet tb = ExcelEngine.openSheet(file_path, src.table_name);
            if (null == tb) {
                System.err.println(
                    String.format("[WARNING] open data source \"%s\" or table %s.", src.file_path, src.table_name)
                );
                continue;
            }

            // 公式支持
            FormulaEvaluator formula = null;
            if (ProgramOptions.getInstance().enableFormular) {
                formula = tb.getWorkbook().getCreationHelper().createFormulaEvaluator();
            }

            // 根据第一个表建立名称关系表
            if (nameMap.isEmpty()) {
                int key_row = scfg.getKey().getRow() - 1;
                Row row = tb.getRow(key_row);
                if (null == row) {
                    System.err.println("[ERROR] get description name row failed");
                    return -53;
                }
                for (int i = src.data_col - 1; i < row.getLastCellNum() + 1; ++i) {
                    DataContainer<String> k = ExcelEngine.cell2s(row, i, formula);
                    nameMap.put(IdentifyEngine.n2i(k.get()), i);
                }
            }

            DataSheetInfo res = new DataSheetInfo();
            res.table = tb;
            res.formula = formula;
            res.next_index = src.data_row - 1;
            res.last_row_number = tb.getLastRowNum();
            res.current_row = null;

            tables.add(res);

            // 记录数量计数
            recordNumber += res.last_row_number - src.data_row + 2;
        }

        return 0;
    }

    @Override
    public boolean next() {
        while(true) {
            if (null != current) {
                current.current_row = null;
            }

            // 当前行超出
            if (null != current && current.next_index > current.last_row_number) {
                current = null;
            }

            if (null == current && tables.isEmpty()) {
                return false;
            }

            if (null == current) {
                current = tables.removeFirst();
            }

            if (null == current) {
                return false;
            }

            current.current_row = current.table.getRow(current.next_index);
            ++current.next_index;

            // 过滤空行
            if (null != current.current_row) {
                break;
            }
        }

        return null != current && null != current.current_row;
    }


    @Override
    public <T> DataContainer<T> getValue(String ident, T ret_default) throws ConvException {
        DataContainer<T> ret = new DataContainer<T>();
        ret.value = ret_default;

        int index = nameMap.getOrDefault(ident, -1);
        if (index < 0)
            return ret;

        if (ret_default instanceof Integer) {
            DataContainer<Long> dt = ExcelEngine.cell2i(current.current_row, index, current.formula);
            ret.valid = dt.valid;
            ret.value = (T)Integer.valueOf(dt.value.intValue());
            return ret;
        } else if (ret_default instanceof Long) {
            DataContainer<Long> dt = ExcelEngine.cell2i(current.current_row, index, current.formula);
            ret.valid = dt.valid;
            ret.value = (T)Long.valueOf(dt.value.longValue());
            return ret;
        } else if (ret_default instanceof Short) {
            DataContainer<Long> dt = ExcelEngine.cell2i(current.current_row, index, current.formula);
            ret.valid = dt.valid;
            ret.value = (T)Short.valueOf(dt.value.shortValue());
            return ret;
        } else if (ret_default instanceof Byte) {
            DataContainer<Long> dt = ExcelEngine.cell2i(current.current_row, index, current.formula);
            ret.valid = dt.valid;
            ret.value = (T)Byte.valueOf(dt.value.byteValue());
            return ret;
        } else if (ret_default instanceof Double) {
            DataContainer<Double> dt = ExcelEngine.cell2d(current.current_row, index, current.formula);
            ret.valid = dt.valid;
            ret.value = (T)Double.valueOf(dt.value.doubleValue());
            return ret;
        } else if (ret_default instanceof Float) {
            DataContainer<Double> dt = ExcelEngine.cell2d(current.current_row, index, current.formula);
            ret.valid = dt.valid;
            ret.value = (T)Float.valueOf(dt.value.floatValue());
            return ret;
        } else if (ret_default instanceof Boolean) {
            ret = (DataContainer<T>) ExcelEngine.cell2b(current.current_row, index, current.formula);
        } else if (ret_default instanceof String) {
            ret = (DataContainer<T>) ExcelEngine.cell2s(current.current_row, index, current.formula);
        } else {
            System.err.println("[ERROR] default value not supported");
        }

        return ret;
    }

    @Override
    public int getRecordNumber() {
        return recordNumber;
    }

    @Override
    public boolean checkName(String _name) {
        return nameMap.containsKey(_name);
    }

    @Override
    public HashMap<String, String> getMacros() {
        return macros;
    }
}
