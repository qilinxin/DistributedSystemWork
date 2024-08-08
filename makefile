JAVAC = javac

# 指定源文件
SRCS = $(wildcard src/main/java/*.java)

# 生成的类文件
CLASSES = $(SRCS:.java=.class)

# 默认目标
all: $(CLASSES)

# 清理生成的文件
# clean:
#     rm -f src/main/java/com/example/*.class

# 规则
src/main/java/com/example/%.class: src/main/java/com/example/%.java
    $(JAVAC) $<git