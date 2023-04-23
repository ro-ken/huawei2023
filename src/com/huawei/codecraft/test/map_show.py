import matplotlib.pyplot as plt
import numpy as np

# %%

input_path_map = '2原始.txt'  # 地图文件

with open(input_path_map) as file:
    content_map = file.readlines()

# 障碍物坐标
wall_x = []
wall_y = []
# 背景坐标
base_x = []
base_y = []
# 工作台坐标
station_x_blue = []
station_y_blue = []
station_x_red = []
station_y_red = []
# 机器人初始坐标
robot_x_blue = []
robot_y_blue = []
robot_x_red = []
robot_y_red = []
# 攻击位置坐标 需要人工填写
attack_x = [10]
attack_y = [40]

# %%
# 背景点转化为现实坐标
for i in range(100):
    for j in range(100):
        base_x.append(0.5 * j + 0.25)
        base_y.append(-0.5 * i + 49.75)

# 障碍物转换为现实坐标
for i in range(100):
    # print(content_map[i].__len__())
    for j in range(100):
        x = 0.5 * j + 0.25
        y = -0.5 * i + 49.75
        if content_map[i][j] == '#':
            wall_x.append(x)
            wall_y.append(y)
        elif content_map[i][j] == 'A':
            robot_x_blue.append(x)
            robot_y_blue.append(y)
        elif content_map[i][j] == 'B':
            robot_x_red.append(x)
            robot_y_red.append(y)
        elif content_map[i][j].isdigit():
            station_x_blue.append(x)
            station_y_blue.append(y)
        elif content_map[i][j].islower():
            station_x_red.append(x)
            station_y_red.append(y)

# %%
# 控制网格参数
xmin = 0
xmax = 50.5
dx = 2.5
ymin = 0
ymax = 50.5
dy = 2.5

plt.rcParams['font.family'] = ['sans-serif']
plt.rcParams['font.sans-serif'] = ['SimHei']
plt.figure(figsize=(10, 10))
plt.xlim((-0.5, 50.5))
plt.ylim((-0.5, 50.5))
plt.scatter(base_x, base_y, s=20, color='gainsboro')  # 背景
plt.scatter(wall_x, wall_y, s=20, color='k')  # 障碍物

plt.scatter(station_x_blue, station_y_blue, s=30, color='deepskyblue')  # 工作台
plt.scatter(station_x_red, station_y_red, s=30, color='orange')  # 工作台
plt.scatter(robot_x_blue, robot_y_blue, s=30, color='b')  # 机器人
plt.scatter(robot_x_red, robot_y_red, s=30, color='r')  # 机器人
plt.scatter(attack_x, attack_y, s=30, color='limegreen')  # 攻击点

plt.grid(True, color='k')
plt.xticks(np.arange(xmin, xmax, dx))  # x轴刻度，以2.5为一格
plt.yticks(np.arange(ymin, ymax, dy))  # y轴刻度，以2.5为一格
plt.show()
