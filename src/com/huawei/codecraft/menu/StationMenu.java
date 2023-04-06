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
        //第一位，stationID，第二位 “empty”/ "full" 第三位 目的地的坐标 ，第四位 路径，以下是示例
//        setPath(1,"empty",new Point(1,2),"[(2.25, 38.75), (3.25, 37.75), (3.25, 35.25), (5.25, 33.75), (6.25, 32.75)]");

//        map1();

//        path = "[(2.25, 38.75), (3.25, 37.75), (3.25, 35.25), (5.25, 33.75), (6.25, 32.75)]";
//        setPath(1,"empty",new Point(1,2),path);
    }

    private static void map1() {
        setPath(0,"empty",new Point(20.75, 16.75),"[(23.75, 47.25), (18.25, 26.25), (20.75, 16.75)]");
        setPath(13,"empty",new Point(2.75, 31.25),"[(27.25, 31.25), (26.75, 30.75), (20.75, 27.0), (17.75, 28.25), (2.75, 31.25)]");
        setPath(13,"empty",new Point(47.25, 31.25),"[(27.25, 31.25), (29.25, 26.75), (30.25, 27.0), (32.75, 28.25), (47.25, 31.25)]");
        setPath(13,"empty",new Point(2.75, 33.25),"[(27.25, 31.25), (26.75, 30.75), (20.75, 27.0), (17.75, 28.25), (2.75, 33.25)]");
        setPath(13,"empty",new Point(47.25, 33.25),"[(27.25, 31.25), (29.25, 26.75), (30.25, 27.0), (32.75, 28.25), (47.25, 33.25)]");
        setPath(17,"empty",new Point(2.75, 29.25),"[(21.75, 27.25), (20.75, 27.0), (17.75, 28.25), (2.75, 29.25)]");
        setPath(17,"empty",new Point(47.25, 29.25),"[(21.75, 27.25), (29.5, 26.75), (32.75, 28.25), (47.25, 29.25)]");
        setPath(17,"empty",new Point(2.75, 33.25),"[(21.75, 27.25), (20.75, 27.0), (17.75, 28.25), (2.75, 33.25)]");
        setPath(17,"empty",new Point(47.25, 33.25),"[(21.75, 27.25), (29.5, 26.75), (32.75, 28.25), (47.25, 33.25)]");
        setPath(18,"empty",new Point(2.75, 29.25),"[(29.25, 27.25), (20.75, 27.0), (17.75, 28.25), (2.75, 29.25)]");
        setPath(18,"empty",new Point(2.75, 31.25),"[(29.25, 27.25), (20.75, 27.0), (17.75, 28.25), (2.75, 31.25)]");
        setPath(18,"empty",new Point(47.25, 29.25),"[(29.25, 27.25), (29.5, 26.75), (32.75, 28.25), (47.25, 29.25)]");
        setPath(18,"empty",new Point(47.25, 31.25),"[(29.25, 27.25), (29.5, 26.75), (32.75, 28.25), (47.25, 31.25)]");
        setPath(21,"empty",new Point(23.75, 47.25),"[(21.75, 22.75), (21.25, 26.75), (20.25, 27.0), (19.75, 28.25), (23.75, 47.25)]");
        setPath(26,"empty",new Point(23.75, 47.25),"[(25.25, 14.75), (25.25, 17.25), (21.25, 26.75), (20.25, 27.0), (19.75, 28.25), (23.75, 47.25)]");
        setPath(0,"full",new Point(20.75, 16.7),"[(23.75, 47.25), (18.25, 26.25), (20.75, 16.75)]");
        setPath(0,"full",new Point(29.75, 16.75),"[(23.75, 47.25), (32.25, 26.25), (29.75, 16.75)]");
        setPath(9,"full",new Point(27.25, 31.25),"[(2.75, 33.25), (22.75, 17.25), (25.25, 20.25), (27.25, 31.25)]");
        setPath(10,"full",new Point(21.75, 27.25),"[(47.25, 33.25), (27.75, 17.25), (25.25, 20.25), (21.75, 27.25)]");
        setPath(11,"full",new Point(29.25, 27.25),"[(2.75, 31.25), (22.75, 17.25), (25.25, 20.25), (29.25, 27.25)]");
        setPath(11,"full",new Point(27.25, 31.25),"[(2.75, 31.25), (22.75, 17.25), (25.25, 20.25), (27.25, 31.25)]");
        setPath(12,"full",new Point(23.75, 47.25),"[(23.25, 31.25), (23.25, 17.25), (21.75, 17.75), (19.75, 21.25), (18.25, 27.25), (23.75, 47.25)]");
        setPath(15,"full",new Point(29.25, 27.25),"[(2.75, 29.25), (22.75, 17.25), (25.25, 20.25), (29.25, 27.25)]");
        setPath(16,"full",new Point(23.75, 47.25),"[(47.25, 29.25), (45.25, 34.75), (41.25, 42.75), (23.75, 47.25)]");
        setPath(16,"full",new Point(21.75, 27.25),"[(47.25, 29.25), (27.75, 17.25), (25.25, 20.25), (21.75, 27.25)]");
        setPath(26,"full",new Point(2.75, 12.25),"[(25.25, 14.75), (23.25, 14.75), (2.75, 12.25)]");
        setPath(26,"full",new Point(47.25, 12.25),"[(25.25, 14.75), (27.25, 14.75), (47.25, 12.25)]");
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
