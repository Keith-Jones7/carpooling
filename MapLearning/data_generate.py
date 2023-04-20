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
date = '2023-03-07_pm'
file_name = 'D:\\1.T3 Work\\Code\\Python\\MapLearning\\input\sample_ds_' + date + '.csv'
LNG = 94403.94
LAT = 111319.49
data = pd.read_csv(file_name)
data = data[['dest_lat','dest_lng','origin_lat','origin_lng']]
dest_lats = list(data['dest_lat'])
dest_lngs = list(data['dest_lng'])
orig_lats = list(data['origin_lat'])
orig_lngs = list(data['origin_lng'])
api = "http://gateway.t3go.com.cn/gis-map-api/lbs/v2/distance/mto"
def get_dist(dest_lat: float, dest_lng: float, origin_lat: float, origin_lng: float) -> str:
    data = {
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
    json_str = json.dumps(data, indent=4)
    result = requests.post(api, json=json.loads(json_str)).text
    data = json.loads(result)
    dist = []
    for sub_data in data['data']['items']:
        dist.append(sub_data['distance'])
    return dist
def get_linear_dist(dest_lat, dest_lng, origin_lat, origin_lng):
    lng_gap = (dest_lng - origin_lng) * LNG
    lat_gap = (dest_lat - origin_lat) * LAT
    return abs(lng_gap) + abs(lat_gap)
def multi_thread():
    # 使用 ThreadPoolExecutor 创建一个线程池
    with concurrent.futures.ThreadPoolExecutor() as executor:
        # 使用 executor.map() 在函数（在本例中为multiply_by_two）上执行多线程计算
        results = list(executor.map(get_dist, dest_lats, dest_lngs, orig_lats, orig_lngs))
    results = [x[0] for x in results]
    return results


results = []
for sub_data in data.iterrows():
    sub_data = sub_data[1]
    results.append(get_linear_dist(sub_data['dest_lat'], sub_data['dest_lng'], sub_data['origin_lat'], sub_data['origin_lng']))
    
data['dist'] = results
data.to_csv("D:/1.T3 Work/Code/Python/MapLearning/output/train3.csv", index=False)
# 现在 "results" 包含multiply_by_two函数的已计算值的列表 [2, 4, 6, 8, 10]

