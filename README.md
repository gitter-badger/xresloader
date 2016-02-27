xresloader
==========

[![Join the chat at https://gitter.im/xresloader/xresloader](https://badges.gitter.im/xresloader/xresloader.svg)](https://gitter.im/xresloader/xresloader?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

快速实现一个简单暴力功能强大的导表工具

构建环境     | Linux (Oracle JDK 8) |
-------------|----------------------|
当前构建状态 | [![Build Status](https://travis-ci.org/xresloader/xresloader.svg?branch=master)](https://travis-ci.org/xresloader/xresloader) |


编译和打包
======

+ 本项目使用[apache maven](https://maven.apache.org/)管理包依赖和打包构建流程。
+ JDK 需要1.8或以上版本

```bash
# 编译
mvn compile
# 打包
mvn package
```

以上命令会自动下载依赖文件、包和插件。

编译完成后，输出的结果默认会放在 ***target*** 目录下。

更新依赖包
------
编译和打包见 [doc/INSTALL.md](安装说明\(doc/INSTALL.md\))


工具命令行参数
======
执行方式    java -jar xresloader.jar [参数...]

比如：（生成源和结果在sample目录下）

```bash
# Excel=>二进制（按协议） 
java -client -jar xresloader.jar -t bin -p protobuf -f kind.pb -s 资源转换示例.xlsx -m scheme_kind

# Excel=>Lua，并重命名输出文件 
java -client -jar xresloader.jar -t lua -p protobuf -f kind.pb --pretty 4 -s 资源转换示例.xlsx -m scheme_kind -n "/(?i)\.bin$/\.lua/"

# Excel=>MsgPack二进制，并重命名输出文件 
java -client -jar xresloader.jar -t msgpack -p protobuf -f kind.pb -s 资源转换示例.xlsx -m scheme_kind -n "/(?i)\.bin$/\.msgpack.bin/"

# 输出文件重命名+输出json格式+多次转表（多个-m参数）
java -client -jar xresloader.jar -t json -p protobuf -f kind.pb -n "/(?i)\.bin$/\.json/" -s 资源转换示例.xlsx -m scheme_kind -m scheme_upgrade

# Excel=>Xml，并重命名输出文件 
# Excel=>Xml，数据源是ini文件, 并重命名输出文件 
# Excel=>Xml，数据源是json文件, 并重命名输出文件 
echo "
-t xml -p protobuf -f kind.pb -s 资源转换示例.xlsx -m scheme_kind -n '/(?i)\.bin$/\.xml/'
-t xml -p protobuf -f kind.pb --pretty 2 -s 资源转换示例.scheme.ini -m scheme_kind -n '/(?i)\.bin$/\.xml/'
-t xml -p protobuf -f kind.pb -s 资源转换示例.scheme.json -m scheme_kind -n '/(?i)\.bin$/\.xml/'
-t lua -p protobuf -f kind.pb --pretty 2 -c kind_const.lua --lua-global
" > params.txt && cat params.txt | java -client -jar xresloader.jar --stdin

# 注意最后这个命令必须使用bash或sh，如果bash的编码是UTF-8在Windows下会因为编码错误而找不到文件，所以需要先把params.txt的编码转为GBK
```

可用参数列表
------

|          参数选项           |         描述        |                   说明                                                                                     |
|-----------------------------|---------------------|------------------------------------------------------------------------------------------------------------|
|-h --help                    | 帮助信息            | 显示帮助和支持的参数列表                                                                                   |
|-t --output-type             | 输出类型            | bin（默认值）,lua,msgpack,json,xml                                                                         |
|-p --proto                   | 协议描述类型        | protobuf(默认值),capnproto(暂未实现),flatbuffer(暂未实现)                                                  |
|-f --proto-file              | 协议描述文件        |                                                                                                            |
|-o --output-dir              | 输出目录            | 默认为当前目录                                                                                             |
|-d --data-src-dir            | 数据源根目录        | 默认为当前目录                                                                                             |
|-s --src-file                | 数据源描述文件      | 后缀可以是 .xls, .xlsx, .cvs, .xlsm, .ods, .ini, .cfg, .conf, .json                                        |
|-m --src-meta                | 数据源描述表        | 可多个                                                                                                     |
|-v --version                 | 打印版本号          |                                                                                                            |
|-n --rename                  | 重命名输出文件名    | 正则表达式 （如：/(?i)\\.bin$/\\.lua/）                                                                    |
|-c --const-print             | 输出协议描述中的常量| 参数为字符串，表示输出的文件名                                                                             |
|--pretty                     | 格式化输出          | 参数为整数，0代表关闭美化输出功能，大于0表示格式化时的缩进量                                               |
|--enable-excel-formular      | 开启Excel公式支持   | 默认开启，使用公式会大幅减慢转表速度                                                                       |
|--disable-excel-formular     | 关闭Excel公式支持   | 关闭公式会大幅加快转表速度                                                                                 |
|--disable-empty-list         | 禁止空列表项        | 默认开启，禁止空列表项，自动删除Excel中的未填充数据，不会转出到输出文件中                                  |
|--enable-empty-list          | 开启空列表项        | 开启空列表项，未填充数据将使用默认的空值来填充，并转出到输出文件中                                         |     
|--stdin                      | 通过标准输入批量转表| 通过标准输入批量转表，参数可上面的一样,每行一项，字符串参数可以用单引号或双引号包裹，但是都不支持转义      |
|--lua-global                 | lua输出写到全局表   | 输出协议描述中的常量到Lua脚本时，同时导入符号到全局表_G中（不会影响配置数据）                              |    
|--xml-root                   | xml输出的根节点tag  | 输出格式为xml时的根节点的TagName                                                                           |    

 
协议类型
------
1. **protobuf**     （已实现）
2. **capnproto**    （暂未支持:目前capnproto还没有提供java下通过反射打解包数据的方法，暂时放弃支持）
3. **flatbuffer**   （暂未支持:目前flatbuffers还没有提供java下通过反射打解包数据的方法，暂时放弃支持）
                
                
数据源描述文件说明(根据后缀判断类型有不同读取方式)
------
|     数据源描述文件后缀      |                                  数据源描述表                                  |           说明           |
|-----------------------------|--------------------------------------------------------------------------------|--------------------------|
|         .xls,.xlsx          | 视作Excel文件，数据源描述表为Excel内的Sheet名称                                |已实现, 非微软格式尚未测试|
|     .ini,.conf,.cfg         | 视作ini文件，数据源描述表为ini内的Section名称                                  |已实现(不支持自动合表)   |
|          .json              | 视作json文件，数据源描述表为json内的第一层子节点名称                              |已实现(必须是UTF-8编码,不支持自动合表)   |
|          .xml               | 视作xml文件，数据源描述表为xml内的根节点下的子节点TagName，并且只取第一个            |(暂未支持)                |


数据源描述表配置项及示例
======
|     字段     |                        简介                                                            |           主配置           |     次配置   |   补充配置   |     说明     |
|--------------|----------------------------------------------------------------------------------------|----------------------------|--------------|--------------|--------------|
|DataSource    | 配置数据源(主配置:文件路径,次配置:表名,补充配置:起始行号，列号)                        |  ./资源转换示例.xlsx       | kind         |  3,1         |   **必须**，可多个。多个则表示把多个Excel表数据合并再生成配置输出，这意味着这多个Excel表的描述Key的顺序和个数必须相同   |
|MacroSource   | 元数据数据源(主配置:文件路径,次配置:表名,补充配置:起始行号，列号)                      |  ./资源转换示例.xlsx       | macro        |  2,1         |    *可选*    |
|编程接口配置  |
|ProtoName     | 协议描述名称                                                                           |   role_cfg                 |              |              |   **必须**   |
|OutputFile    | 输出文件                                                                               |   role_cfg.bin             |              |              |   **必须**   |
|KeyRow        | 字段名描述行                                                                           |  2                         |              |              |   **必须**   |
|KeyCase       | 字段名大小写                                                                           | 小写                       |              |              |大写/小写/不变|
|KeyWordSplit  | 字段名分词字符                                                                         | _                          |              |              |    *可选*    |
|KeyPrefix     | 字段名固定前缀                                                                         |                            |              |              |    *可选*    |
|KeySuffix     | 字段名固定后缀                                                                         |                            |              |              |    *可选*    |
|KeyWordRegex  | 分词规则(判断规则,移除分词符号规则,前缀过滤规则)                                       | [A-Z_\$ \t\r\n]            | [_\$ \t\r\n] | [a-zA-Z_\$]  | 正则表达式*(可选)*|
|Encoding      | 编码转换                                                                               | UTF-8                      |              |              |注：Google的protobuf库的代码里写死了UTF-8(2.6.1版本)，故而该选项对Protobuf的二进制输出无效|


输出格式说明
======
|          输出格式参数       |                                  输出格式说明                                                                                       |           说明                                    |
|-----------------------------|-------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------|
|           bin               | 基于协议的二进制文件,不同的协议类型(-p参数)输出的二进制不一样,一般是header+body,body中有转出的数据列表, 协议格式见header文件夹      | 示例见 [sample](sample/role_cfg.bin) (protobuf)                |
|           lua               | 转出为格式化的lua文件(易读), 一般格式为 return {\[1\] = 转表头信息, \[协议结构名称\] = {数据列表} }                                 | 示例见[sample](sample/role_cfg.lua)          |
|          msgpack            | 转出为使用[MsgPack](http://msgpack.org/)打包的二进制文件,内含的第一个message是转表头信息，后面紧跟数据，可以用任何支持得客户端解包  | 示例见[sample](sample/role_cfg.msgpack.bin)  |
|           json              | 转出为json文件,一般格式为 \[ {转表头信息}, {协议结构名称 : \[ 转出的数据 \] } \]                                             | 示例见[sample](sample/role_cfg.json)         |
|           xml               | 转出为xml文件,一般格式为&lt;root&gt;&lt;header&gt;转表头信息&lt;/header&gt;&lt;body&gt;&lt;协议结构名称&gt;数据内容&lt;/协议结构名称&gt;&lt;/body&gt;&lt;/root&gt; | 示例见[sample](sample/role_cfg.xml)  |


**注意：** Xml输出格式中，列表元素的结构是*&lt;配置名称&gt;&lt;item&gt;数据1&lt;/item&gt;&lt;item&gt;数据2&lt;/item&gt;...&lt;/配置名称&gt;* 里层结构固定tagName为item。目的是方便通过xpath查找（保证和父节点不重名）

关于加载导出的数据
======

转出的数据都采用header+data_block的形式。本工具并不规定怎么读取转表导出的数据，开发者可以按照转出的数据规则自由操作。

但是为了使用方便，在[loader-binding](loader-binding)里提供了几种基本的读表方式。

protobuf协议
------
1. 如果你使用官方的protobuf或protobuf-lite，可以使用[loader-binding/cxx](loader-binding/cxx)来加载配置
> sample 参见: [sample/cxx/read_kind_sample.cpp](sample/cxx/read_kind_sample.cpp)

2. 如果你使用云峰的[pbc](https://github.com/cloudwu/pbc)，可以使用[loader-binding/pbc](loader-binding/pbc)来加载配置
> 这个加载器会依赖 [https://github.com/owent-utils/lua/tree/master/src](https://github.com/owent-utils/lua/tree/master/src) 里的部分内容。
> 
> 需要使用pbc先加载[header/pb_header.pb](header/pb_header.pb)文件
> 
> pbc_config_manager:load_buffer_kv(协议名, 二进制, function(序号, 转出的lua table) return key的值 end) -- 读取key-value型数据接口
> 
> pbc_config_manager:load_buffer_kl(协议名, 二进制, function(序号, 转出的lua table) return key的值 end) -- 读取key-list型数据接口

其他输出格式
------
1. **lua**格式输出可以按[loader-binding/lua](loader-binding/lua)的说明读取。
> 这个加载器会依赖 [https://github.com/owent-utils/lua/tree/master/src](https://github.com/owent-utils/lua/tree/master/src) 里的部分内容。
> 
> conf_manager:load_kv(require的路径, function(序号, 转出的lua table) return key的值 end) -- 读取key-value型数据接口
> 
> conf_manager:load_kl(require的路径, function(序号, 转出的lua table) return key的值 end) -- 读取key-list型数据接口

2. [MsgPack](http://msgpack.org/)的读取的语言和工具很多，任意工具都能比较简单地读出数据，故而不再提供读取工具
3. **Json**的读取的语言和工具很多，任意工具都能比较简单地读出数据，故而不再提供读取工具
3. **Xml**的读取的语言和工具很多，任意工具都能比较简单地读出数据，故而不再提供读取工具

使用注意事项
======
1. Excel里编辑过的单元格即便删除了也会留下不可见的样式配置，这时候会导致转出的数据有空行。可以通过在Excel里删除行解决
2. Excel里的日期时间类型转成协议里整数时会转为Unix时间戳，但是Excel的时间是以1900年1月0号为基准的，这意味着如果时间格式是hh:mm:dd的话，49:30:01会被转为1900-1-2 1:31:01。时间戳会是一个很大的负数
