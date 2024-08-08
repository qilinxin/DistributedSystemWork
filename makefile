# 指定编译器
JAVAC = javac

# 指定源文件目录
SRC_DIR = src/main/java

# 获取所有 .java 文件
SRCS = $(SRC_DIR)/*.java

# 默认目标
all: classes

# 编译所有 .java 文件为 .class 文件
classes: $(SRCS)
	$(JAVAC) -d $(SRC_DIR) $(SRCS)

# 清理生成的 .class 文件
clean:
	rm -f $(SRC_DIR)/*.class
