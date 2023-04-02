package com.huawei.codecraft.test;

import java.io.*;
import java.util.*;

public class test {
	public static Map<Integer, Pos> robotPos = new HashMap<>();;  // 记录起始机器人的位置

    public static void main(String[] args) {
        int[][] maps = new int[100][100];
        readMaps(maps);

//		Point startPosition = new Point(21.75, 30.25);
		Point startPosition = new Point(32.75, 29.25);
		Point endPosition = new Point(28.25, 23.25);
		// Mapinfo mapInfo = new Mapinfo(maps);
		// mapInfo.printMapFull();
        // 获取当前时间
		long startTime = System.currentTimeMillis();

		Mapinfo mapinfo = new Mapinfo(maps);
		Astar aStar = new Astar(Mapinfo.mapInfoFull, startPosition, endPosition);

		// aStar.board.printBoard();
		aStar.search();


        // 获取当前时间
		long endTime = System.currentTimeMillis();
        // 计算代码运行时间并输出结果
		long elapsedTime = endTime - startTime;
		System.out.println("代码运行时间：" + elapsedTime + "ms");

        mapinfo.getConnectedArea(robotPos);

		Map<Integer, ArrayList<Integer>> robotId = mapinfo.getConnectedRobotsId();
		Map<Integer, ArrayList<Integer>> stationId = mapinfo.getConnectedStationsId();
        ArrayList<Pos> resultList = aStar.getResultList();
		ArrayList<Point> result = aStar.getResult(true);
		System.out.println(robotId);
		System.out.println(stationId);
		ArrayList<Integer> robot = robotId.get(0);
		ArrayList<Integer> station = stationId.get(0);

		System.out.println("robotId size " + robotId.size());
		System.out.println("stationId size " + stationId.size());
		System.out.println("robot size " + robot.size());
		System.out.println("station size " + station.size());
        System.out.println("size: " + resultList.size());
		System.out.println("size: " + result.size());
        printMap(maps, resultList);

    }

    /**
	 * 打印地图
	 */
	public static void printMap(int[][] maps, ArrayList<Pos> pos) {
		for (Pos postion : pos) {
            maps[postion.x][postion.y] = 2;
        }
		try {
            File file = new File("result.txt"); // 文件路径
            FileOutputStream fos = new FileOutputStream(file);

			for (int i = 0; i < maps.length; i++) {
				for (int j = 0; j < maps[i].length; j++) {
					String data;
					if (maps[i][j] == -2){
						data = String.valueOf(1);
					} else if (maps[i][j] == 2){
						data = String.valueOf(2);
					} else {
						data = " ";
					}
					byte[] bytes = data.getBytes();
                    fos.write(bytes);
				}
				String lineSeparator = System.lineSeparator();
                byte[] bytes = lineSeparator.getBytes();
                fos.write(bytes);
			}
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
	}

    /**
	 * 读取地图
	 */
    public static void readMaps(int[][] maps) {
        try {
            File file = new File("C:\\Users\\rq886\\Desktop\\fusai\\WindowsRelease\\maps\\2.txt"); // 文件路径
            FileInputStream fis = new FileInputStream(file);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);
            
            String line;
			int row = 0, stationId = 0, robotId = 100;
            while ((line = br.readLine()) != null) {
                for (int col = 0; col < line.length(); col++) {
					char c = line.charAt(col);
					if (c == '.') {
						maps[row][col] = -1;
					} else if (c == '#') {
						maps[row][col] = -2;
					} else if (c == 'A'){
						// System.out.println("A: " + row + " " + col);
						robotPos.put(Integer.valueOf(robotId), new Pos(row, col));
						maps[row][col] = robotId++;

					} else if (c <= '9' && c >= '0') {
						// System.out.println("station: " + c + " " + row + " " + col);
						maps[row][col] = stationId++;
					}
				}
				row++;
            }
            
            br.close();
            isr.close();
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
