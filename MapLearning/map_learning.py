# -*- coding: utf-8 -*-
"""
Created on Thu Apr 20 15:06:40 2023

@author: zhangkj1
"""

import torch
import torch.nn as nn
import torch.optim as optim
from torch.utils.data import Dataset, DataLoader
import numpy as np
import pandas as pd
import math
import matplotlib.pyplot as plt
import random
class ODDataset(Dataset):
    def __init__(self, data, labels):
        self.data = data
        self.labels = labels

    def __len__(self):
        return len(self.data)

    def __getitem__(self, index):
        x = self.data[index]
        y = self.labels[index]

        return x, y

loadings = pd.read_csv("output/train.csv")

data = np.array(loadings[['origin_lat', 'origin_lng', 'dest_lat', 'dest_lng']])
data_mean = torch.tensor(np.mean(data, axis=0), dtype=torch.float64)  # 计算每个数据点（经度、纬度）的均值
data_std = torch.tensor(np.std(data, axis=0), dtype=torch.float64)  # 计算每个数据点（经度、纬度）的标准差
labels = np.array(loadings['dist'])  # 输入空间距离标签
dataset = ODDataset(data, labels)
dataloader = DataLoader(dataset, batch_size=64, shuffle=True)

class NeuralNetwork(nn.Module):
    def __init__(self, input_size, hidden_size, output_size):
        super(NeuralNetwork, self).__init__()
        self.fc1 = nn.Linear(input_size, hidden_size)
        self.fc2 = nn.Linear(hidden_size,hidden_size)
        self.fc3 = nn.Linear(hidden_size, output_size)
        self.activate = nn.LeakyReLU()

    def forward(self, x):
        x = (x - data_mean) / data_std
        x = self.fc1(x)
        x = self.activate(x)
        x = self.fc2(x)
        x = self.activate(x)
        x = self.fc3(x)
        return x

input_size = 4  # 对于OD经纬度数据，每个数据点包含4个值（O点的经度、纬度和D点的经度、纬度）
hidden_size = 64  # 可以调整隐藏层大小
output_size = 1  # 输出1个值，空间距离

model = NeuralNetwork(input_size, hidden_size, output_size)
model.double()
loss_function = nn.MSELoss()
optimizer = optim.Adam(model.parameters(), lr=0.001)

epochs = 100
for epoch in range(epochs):
    for i, (inputs, targets) in enumerate(dataloader):
        inputs = inputs.clone().detach()
        targets = targets.clone().detach()
        targets = targets.unsqueeze(dim=1)
        optimizer.zero_grad()
        outputs = model(inputs)
        loss = loss_function(outputs, targets)
        loss.backward()
        optimizer.step()
    print(f"Epoch: {epoch+1}, Loss: {loss.item()}")

def eval_model(model, inputs):
    model.eval()
    outputs = []
    for input__ in inputs :     
        input__ = torch.tensor(input__, dtype=torch.float64)
        output = float(model(input__))
        outputs.append(output)
    return outputs

def get_linear_dist(dest_lat, dest_lng, origin_lat, origin_lng):
    LNG = 94403.94
    LAT = 111319.49
    lng_gap = (dest_lng - origin_lng) * LNG
    lat_gap = (dest_lat - origin_lat) * LAT
    return math.sqrt(lng_gap * lng_gap + lat_gap * lat_gap)



output__ = eval_model(model, data)

inputs = torch.tensor([0,0,0,0], dtype=torch.float64) 
convert = torch.jit.trace(model, inputs)
convert.save("model/NetMap.pt")

