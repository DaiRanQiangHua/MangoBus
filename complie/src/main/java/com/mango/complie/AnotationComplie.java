package com.mango.complie;

import com.google.auto.service.AutoService;
import com.mango.anotation.BindView;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.JavaFileObject;

/**
 * @Description TODO(注解处理器 帮我们生成文件并且写文件)
 * @author Mangoer
 * @Date 2019/5/22 22:37
 * 自定义注解处理器：
 *      1. 继承AbstractProcessor类 并重写process方法
 *      2. 使用@AutoService注解修饰 注册注解处理器
 *  在编译期间，编译器会定位到Java源文件中的注解（因为有RetentionPolicy.CLASS修饰），注解处理器会对其感兴趣的注解进行处理
 *  一个注解处理器只能产生新的源文件，不能修改一个已经存在的源文件
 */
@AutoService(Processor.class)
public class AnotationComplie extends AbstractProcessor {

    //创建Java源文件、Class文件以及其它辅助文件的对象
    Filer mFiler;
    //包含了一些用于操作Element的方法
    Elements mElementUtils;

    /**
     * 初始化方法被注解处理工具调用，并传入参数，这个参数包含了很多有用的工具类
     * 比如Elements、Types、Filer等
     * @param processingEnv
     */
    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        mFiler = processingEnv.getFiler();
        mElementUtils = processingEnv.getElementUtils();
    }

    /**
     * 这个方法很重要，指定这个注解处理器能够处理的注解
     * 返回一个Set集合，里面保存我们希望它处理的注解
     * @return
     */
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> type = new LinkedHashSet<>();
        type.add(BindView.class.getCanonicalName());
        return type;
    }

    /**
     * 指定注解处理器使用的Java版本，通常返回SourceVersion.latestSupported()即可
     * 也可以指定支持某个版本的Java，比如SourceVersion。RELEASE_6
     * @return
     */
    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    /**
     * 实现注解处理器的具体业务逻辑
     * 这里我们就需要编写findViewById等代码
     * @param annotations
     * @param roundEnv
     * @return
     */
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        /**
         * 要知道我们的Java文件都是结构化的数据，最外层就是类声明，里面是全局变量，然后就是方法
         * 这样每一个结点都对应一个标签，比如
         *          类标签---------TypeElement
         *          成员变量标签---VariableElement
         *          方法标签-------ExecutableElement
         *
         *  所以这里拿到的Set集合就是程序中所有使用到BindView注解的结点
         */
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(BindView.class);
        /**
         * 把所有使用到BindView的数据结构化
         * key-----类名
         * value---每个类里使用到BindView的成员变量的集合
         */
        Map<String,List<VariableElement>> eleMap = new HashMap<>();
        /**
         * 遍历所有使用到BindView注解的结点，也就是成员变量
         * 将其与所在类一一对应
         */
        for (Element element : elements) {

            VariableElement variableElement = (VariableElement) element;
            //通过成员变量结点获取上一个结点，也就是拿到类结点，再拿到类名
            String className = variableElement.getEnclosingElement().getSimpleName().toString();
            List<VariableElement> elementList = eleMap.get(className);
            if (elementList == null) {
                elementList = new ArrayList<>();
                eleMap.put(className,elementList);
            }
            elementList.add(variableElement);

        }

        /**
         * 接下来就是重点了，开始写Java文件了
         * Java文件是结构化数据
         */
        Iterator<String> iterator = eleMap.keySet().iterator();
        //写文件对象
        Writer writer = null;
        while (iterator.hasNext()) {
            //拿到每个类的所有成员变量
            String className = iterator.next();
            List<VariableElement> elementList = eleMap.get(className);
            //拿到包名，主要是通过成员变量结点的上一个结点，也就是类结点，获取它的包信息
            String packageName = mElementUtils.getPackageOf(elementList.get(0).getEnclosingElement()).toString();
            /**
             * 创建Java文件
             * createSourceFile---创建Java文件
             * createClassFile----创建Class文件
             * createResource-----创建资源文件
             */
            try {
                JavaFileObject fileObject = mFiler.createSourceFile(className+"$ViewBinder");
                writer = fileObject.openWriter();
                //写类的包名
                writer.write("package " + packageName + ";\n");

                writer.write("\n");
                // 写导入的类
                writer.write("import "  + "com.mango.knife.ViewBinder" + ";\n");

                writer.write("\n");
                //定义类
                writer.write("public class " + className + "$ViewBinder implements ViewBinder<" + packageName + "." + className + ">{" + "\n");
                //定义bind方法 接收参数
                writer.write("    public void bind(" + packageName + "." + className + " target){" + "\n");
                //写findViewById
                for (VariableElement variableElement : elementList) {
                    //获取变量名
                    String name = variableElement.getSimpleName().toString();
                    //获取变量类型
                    TypeMirror typeMirror = variableElement.asType();
                    //获取控件id
                    BindView annotation = variableElement.getAnnotation(BindView.class);
                    int id = annotation.value();
                    writer.write("        target." + name + " = (" + typeMirror + ")target.findViewById(" + id + ");"+"\n");
                }
                writer.write("    }\n");

                //定义unBind方法 接收参数
                writer.write("    public void unBind(" + packageName + "." + className + " target){" + "\n");
                //写findViewById
                for (VariableElement variableElement : elementList) {
                    //获取变量名
                    String name = variableElement.getSimpleName().toString();
                    //获取变量类型
                    TypeMirror typeMirror = variableElement.asType();
                    //获取控件id
                    BindView annotation = variableElement.getAnnotation(BindView.class);
                    int id = annotation.value();
                    writer.write("        target." + name + " = (" + typeMirror + ")target.findViewById(" + id + ");"+"\n");
                }
                writer.write("    }\n");

                writer.write("}");
            } catch (IOException e) {
                e.printStackTrace();
            }finally {
                if (writer != null) {
                    try {
                        writer.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return false;
    }
}
