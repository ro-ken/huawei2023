package com.huawei.codecraft.menu;


import com.huawei.codecraft.Main;
import com.huawei.codecraft.util.Point;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// 手动修改路线接口
public class StationMenu {

    public static void setStationPath(){
        //第一位，stationID，第二位 “empty”/ "full" 第三位 下一个点的坐标 ，第四位 路径，以下是示例
//        setPath(1,"empty",new Point(1,2),"[(2.25, 38.75), (3.25, 37.75), (3.25, 35.25), (5.25, 33.75), (6.25, 32.75)]");
        String path;

//        path = "[(2.25, 38.75), (3.25, 37.75), (3.25, 35.25), (5.25, 33.75), (6.25, 32.75)]";
//        setPath(1,"empty",new Point(1,2),path);
    }

    private static void setPath(int stationID, String status, Point target, String path) {

        Map<Point,ArrayList<Point>> maps;
        if ("empty".equals(status)){
            maps = Main.stations[stationID].paths.emptyPathMap;
        }else if ("full".equals(status)){
            maps = Main.stations[stationID].paths.fullPathMap;
        }else {
            return;
        }
        ArrayList<Point> points = splitPath(path);
        maps.put(target,points);
    }

    private static ArrayList<Point> splitPath(String path) {

        Matcher matcher = Pattern.compile("\\(([^,]+), ([^)]+)\\)").matcher(path);
        ArrayList<Point> points = new ArrayList<>();
        while (matcher.find()) {
            double x = Double.parseDouble(matcher.group(1));
            double y = Double.parseDouble(matcher.group(2));
            Point point = new Point(x,y);
            points.add(point);
        }
        return points;
    }

    public static void main(String[] args) {
//        String input = "[(47.25, 38.75), (47.25, 37.25), (49.25, 37.25), (49.25, 40.25), (47.25, 40.75), (45.25, 40.25)]";
//        Matcher matcher = Pattern.compile("\\(([^,]+), ([^)]+)\\)").matcher(input);
//        List<double[]> coordsList = new ArrayList<>();
//        while (matcher.find()) {
//            double x = Double.parseDouble(matcher.group(1));
//            double y = Double.parseDouble(matcher.group(2));
//            System.out.println(x + ":" + y);
//            coordsList.add(new double[]{x, y});
//        }

    }
}
