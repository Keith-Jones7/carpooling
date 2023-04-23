# -*- coding: utf-8 -*-
"""
Created on Thu Apr 20 17:35:19 2023

@author: zhangkj1
"""

import threading
import json
from typing import List, Tuple
import requests
import concurrent.futures
import pandas as pd
import numpy as np
import math
import time
api = "http://gateway.t3go.com.cn/gis-map-api/lbs/v2/distance/mto"
def get_dist(dest_lat: float, dest_lng: float, origin_lat: float, origin_lng: float) -> str:
    json_sample = {
        "cityCode": "330700",
        "dest": {
            "lat": dest_lat,
            "lng": dest_lng
        },
        "origins": [
            {
                "lat":origin_lat,
                "lng":origin_lng
            }
        ]
    }
    json_str = json.dumps(json_sample, indent=4)
    result = requests.post(api, json=json.loads(json_str)).text
    dist = []
    data = json.loads(result)

    try:
        for sub_data in data['data']['items']:
            dist.append(sub_data['distance'])
    except:
        print(result)
        dist.append(np.nan)
    return dist

def multi_thread(dest_lats, dest_lngs, orig_lats, orig_lngs):
    # 使用 ThreadPoolExecutor 创建一个线程池
    with concurrent.futures.ThreadPoolExecutor() as executor:
        # 使用 executor.map() 在函数（在本例中为multiply_by_two）上执行多线程计算
        results = list(executor.map(get_dist, dest_lats, dest_lngs, orig_lats, orig_lngs))
    results = [x[0] for x in results]
    return results
def get_linear_dist(dest_lat, dest_lng, origin_lat, origin_lng):
    LNG = 94403.94
    LAT = 111319.49
    lng_gap = (dest_lng - origin_lng) * LNG
    lat_gap = (dest_lat - origin_lat) * LAT
    return dest_lat + dest_lng + origin_lat + origin_lng

    
#数据日期
date = '2023-03-09'

#读取文件名
file_name = 'MapLearning/input/sample_ds_' + date + '.csv'

#读取数据
data = pd.read_csv(file_name)
data.dropna(inplace=True)
data = data.iloc[0:20000, :]
#定义每次遍历行数
batch_size = 3000

#获取数据总行数
total_rows = data.shape[0]

#计算遍历次数
num_batches = (total_rows // batch_size) + 1

dist = []
for i in range(num_batches):
    # 计算当前批次的起始行索引和结束行索引
    start_idx = i * batch_size
    end_idx = min((i + 1) * batch_size, total_rows)

    # 获取当前批次的数据
    sub_data = data.iloc[start_idx:end_idx, :]
    dest_lats = list(sub_data['dest_lat'])
    dest_lngs = list(sub_data['dest_lng'])
    orig_lats = list(sub_data['origin_lat'])
    orig_lngs = list(sub_data['origin_lng'])
    dist.extend(multi_thread(dest_lats, dest_lngs, orig_lats, orig_lngs))
    time.sleep(3)
    print("当前阶段已处理" + str(end_idx))

data['dist'] = dist
data.dropna(inplace=True)
data.to_csv("MapLearning/output/train.csv", index=False)

