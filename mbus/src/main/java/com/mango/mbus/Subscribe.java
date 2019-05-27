package com.mango.mbus;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Author: Mangoer
 * Time: 2019/5/15 20:27
 * Version:
 * Desc: TODO(以@interface格式声明注解)
 * 注解其实是类的一种形式
 * 在其上方还要添加几个注解：
 *      @Target：声明该注解的作用域，枚举类ElementType有几个值：
 *                                  FIELD：变量，表明该注解只能作用在变量上
 *                                  PARAMETER：参数，表明该注解只能作用在参数上
 *                                  METHOD：方法，表明该注解只能作用在方法上
 *                                  ANNOTATION_TYPE：注解，表明该注解只能作用在注解类上
 *                                  TYPE：类，表明该注解只能作用在类上
 *      @Retention：声明该注解的生命周期，枚举类RetentionPolicy也有几个值：
 *                                  SOURCE：表明该注解在源码里存在
 *                                  CLASS：表明该注解在编译后的class文件里存在
 *                                  RUNTIME：表明该注解在运行时存在
 *
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Subscribe {
    //定义线程模型，指定默认值
    ThreadMode threadMode() default ThreadMode.POSTING;
    boolean sticky() default false;
}
