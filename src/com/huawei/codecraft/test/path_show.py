from collections import defaultdict
import random

import matplotlib.pyplot as plt

#%%
# 读取result.txt中的地图信息，并转换为现实坐标
import numpy as np

input_path_map = '1.txt'
# 读取工作台之间的路径信息(现实坐标)
input_path = 'path1.txt'
# 是否输出单条路径
isOutputFull = False
isOutputEmpty = False

#%%
with open(input_path_map) as file:
    content_map = file.readlines()

with open(input_path) as file:
    content_path = file.readlines()

# 障碍物坐标
wall_x = []
wall_y = []
# 背景坐标
base_x = []
base_y = []
# 工作台坐标
station_x = []
station_y = []
# 路径坐标
path_x_empty = []
path_y_empty = []
path_x_full = []
path_y_full = []
# 存放读入的数据
path_all_empty = defaultdict(list)
path_all_full = defaultdict(list)

# 背景点转化为现实坐标
for i in range(100):
    for j in range(100):
        base_x.append(0.5 * j + 0.25)
        base_y.append(-0.5 * i + 49.75)

# 障碍物转换为现实坐标
for i in range(100):
    for j in range(100):
        if content_map[i][j] == '#':
            wall_x.append(0.5 * j + 0.25)
            wall_y.append(-0.5 * i + 49.75)
        elif content_map[i][j].isdigit():
            station_x.append(0.5 * j + 0.25)
            station_y.append(-0.5 * i + 49.75)

# 保存所有路径信息[(x,y),(x,y)...(x,y)]
for i in range(len(content_path)):
    info = content_path[i][:-1].split(':')
    stationId = info[0].split(' ')[1]  # 工作台id
    label = info[1].split(' ')[0]  # 记录是empty/full
    info_path = info[2][1:-2].replace('=[', ', ')[:-1].split('], ')
    if info_path[0] == '':
        continue
    path = []
    for j in range(len(info_path)):
        path_list = []
        path_temp = list(eval(info_path[j]))
        path_list.extend(path_temp[1:])  # 中间节点
        # path_list.append(path_temp[0])  # 终点
        path.append(path_list)

    if label == 'empty':
        path_all_empty[stationId] = path
    else:
        path_all_full[stationId] = path

#%%
#控制网格参数
xmin = 0
xmax = 50.5
dx = 2.5
ymin = 0
ymax = 50.5
dy = 2.5

# 随机生成颜色
def rgbrandom():
    rr = random.randint(0, 255)
    gg = random.randint(0, 255)
    bb = random.randint(0, 255)
    rgb = str(rr) + ',' + str(gg) + ',' + str(bb)
    return rgb


# 颜色转化为16进制
def RGB_to_Hex(rgb):
    RGB = rgb.split(',')  # 将RGB格式划分开来
    mycolor = '#'
    for i in RGB:
        num = int(i)
        # 将R、G、B分别转化为16进制拼接转换并大写  hex() 函数用于将10进制整数转换成16进制，以字符串形式表示
        mycolor += str(hex(num))[-2:].replace('x', '0').upper()
    return mycolor

# %%
# 全局画图代码
plt.rcParams['font.family'] = ['sans-serif']
plt.rcParams['font.sans-serif'] = ['SimHei']
plt.figure(figsize=(10, 10))
plt.xlim((-0.5, 50.5))
plt.ylim((-0.5, 50.5))
plt.scatter(base_x, base_y, s=20, color='gainsboro')  # 背景
plt.scatter(wall_x, wall_y, s=20, color='k')  # 障碍物

count = 0  # 记录路线数

for key in path_all_full.keys():
    for i in range(len(path_all_full[key])):
        point_x = []
        point_y = []
        for j in range(len(path_all_full[key][i])):
            pair = path_all_full[key][i][j]
            point_x.append(float(pair[0]))
            point_y.append(float(pair[1]))
        rgb = rgbrandom()  # 随机生成颜色
        RGB_to_Hex(rgb)  # 转换为十六进制
        plt.plot(point_x, point_y, linewidth=2)
        count += 1
        # plt.plot(point_x, point_y, linewidth=2, color='deepskyblue')

for key in path_all_empty.keys():
    for i in range(len(path_all_empty[key])):
        point_x = []
        point_y = []
        for j in range(len(path_all_empty[key][i])):
            pair = path_all_empty[key][i][j]
            point_x.append(float(pair[0]))
            point_y.append(float(pair[1]))
        plt.plot(point_x, point_y, linewidth=2)
        count += 1
        # plt.plot(point_x, point_y, linewidth=2, color='deepskyblue')

plt.scatter(station_x, station_y, s=30, color='r')  # 工作台
plt.grid(True, color='k')
plt.xticks(np.arange(xmin, xmax, dx))  # x轴刻度，以2.5为一格
plt.yticks(np.arange(ymin, ymax, dy))  # y轴刻度，以2.5为一格
plt.show()

# %%
# 单条路线

# 输出文件名信息
mapId = input_path_map[0]
empty_full = ''
stationId = ''
pathId = ''

#%%
if isOutputFull:
    for key in path_all_full.keys():
    # for key in list(path_all_full.keys())[26]:
        for i in range(len(path_all_full[key])):
            point_x = []
            point_y = []
            for j in range(len(path_all_full[key][i])):
                pair = path_all_full[key][i][j]
                point_x.append(float(pair[0]))
                point_y.append(float(pair[1]))

            plt.rcParams['font.family'] = ['sans-serif']
            plt.rcParams['font.sans-serif'] = ['SimHei']
            plt.figure(figsize=(10, 10))
            plt.xlim((-0.5, 50.5))
            plt.ylim((-0.5, 50.5))
            plt.scatter(base_x, base_y, s=20, color='gainsboro')  # 背景
            plt.scatter(wall_x, wall_y, s=20, color='k')  # 障碍物
            plt.plot(point_x, point_y, linewidth=2)
            plt.scatter(station_x, station_y, s=30, color='r')  # 工作台
            plt.scatter(point_x, point_y, s=20, label='full')
            plt.legend()
            plt.grid(True, color='k')
            plt.xticks(np.arange(xmin, xmax, dx))  # x轴刻度，以2.5为一格
            plt.yticks(np.arange(ymin, ymax, dy))  # y轴刻度，以2.5为一格
            empty_full = 'full'
            stationId = key
            pathId = str(i)
            filename = 'map%s-%s-station%s-path%s.png' % (mapId, empty_full, stationId, pathId)
            print(filename)
            plt.title(filename, fontdict={'size': 40, 'weight': 'bold'})
            plt.savefig('G:\\Python_file\\Pycharm_project\\Huawei\\2023Huawei\\path_optimization\\map%s\\%s' % (mapId, filename))
            # plt.show()
            plt.close()

#%%
if isOutputEmpty:
    for key in path_all_empty.keys():
        for i in range(len(path_all_empty[key])):
            point_x = []
            point_y = []
            for j in range(len(path_all_empty[key][i])):
                pair = path_all_empty[key][i][j]
                point_x.append(float(pair[0]))
                point_y.append(float(pair[1]))

            plt.rcParams['font.family'] = ['sans-serif']
            plt.rcParams['font.sans-serif'] = ['SimHei']
            plt.figure(figsize=(10, 10))
            plt.xlim((-0.5, 50.5))
            plt.ylim((-0.5, 50.5))
            plt.scatter(base_x, base_y, s=20, color='gainsboro')  # 背景
            plt.scatter(wall_x, wall_y, s=20, color='k')  # 障碍物
            plt.plot(point_x, point_y, linewidth=2, color='orange')
            plt.scatter(station_x, station_y, s=30, color='r')  # 工作台
            plt.scatter(point_x, point_y, s=20, label='empty', color='orange')
            plt.legend()
            plt.grid(True, color='k')
            plt.xticks(np.arange(xmin, xmax, dx))  # x轴刻度，以2.5为一格
            plt.yticks(np.arange(ymin, ymax, dy))  # y轴刻度，以2.5为一格
            empty_full = 'empty'
            stationId = key
            pathId = str(i)
            filename = 'map%s-%s-station%s-path%s.png' % (mapId, empty_full, stationId, pathId)
            print(filename)
            plt.title(filename, fontdict={'size': 40, 'weight': 'bold'})
            plt.savefig('G:\\Python_file\\Pycharm_project\\Huawei\\2023Huawei\\path_optimization\\map%s\\%s' % (mapId, filename))
            # plt.show()
            plt.close()
