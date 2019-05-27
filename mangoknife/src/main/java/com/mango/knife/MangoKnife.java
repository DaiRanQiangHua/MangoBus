package com.mango.knife;

import android.app.Activity;

/**
 * Author: Mangoer
 * Time: 2019/5/26 21:29
 * Version:
 * Desc: TODO()
 */
public class MangoKnife {

    public static void bind(Activity activity){

        String complieName = activity.getClass().getName()+"$ViewBinder";

        try {
            ViewBinder viewBinder = (ViewBinder) Class.forName(complieName).newInstance();
            viewBinder.bind(activity);

        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void unBind(Activity activity){
        String complieName = activity.getClass().getName()+"$ViewBinder";

        try {
            ViewBinder viewBinder = (ViewBinder) Class.forName(complieName).newInstance();
//            viewBinder.unBind(activity);

        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
