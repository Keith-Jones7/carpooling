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

loadings = pd.read_csv("D:/1.T3 Work/Code/Python/MapLearning/output/train3.csv")
data = np.array(loadings[['origin_lat', 'origin_lng', 'dest_lat', 'dest_lng']])
data_mean = np.mean(data, axis=0)  # 计算每个数据点（经度、纬度）的均值
data_std = np.std(data, axis=0)  # 计算每个数据点（经度、纬度）的标准差
data = (data - data_mean) / data_std  # 输入OD经纬度数据
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
        x = self.fc1(x)
        x = self.activate(x)
        x = self.fc2(x)
        x = self.activate(x)
        x = self.fc3(x)
        return x

input_size = 4  # 对于OD经纬度数据，每个数据点包含4个值（O点的经度、纬度和D点的经度、纬度）
hidden_size = 100  # 可以调整隐藏层大小
output_size = 1  # 输出1个值，空间距离

model = NeuralNetwork(input_size, hidden_size, output_size)
loss_function = nn.MSELoss()
optimizer = optim.Adam(model.parameters(), lr=0.001)

epochs = 100
for epoch in range(epochs):
    for i, (inputs, targets) in enumerate(dataloader):
        inputs, targets = torch.tensor(inputs, dtype=torch.float32), torch.tensor(targets, dtype=torch.float32)
        optimizer.zero_grad()
        outputs = model(inputs)
        loss = loss_function(outputs, targets)
        loss.backward()
        optimizer.step()
    print(f"Epoch: {epoch+1}, Loss: {loss.item()}")

def eval_model(model, inputs):
    model.eval()
    total_loss = 0
    outputs = []
    for input in inputs :     
        input = torch.tensor(input, dtype=torch.float32)
        output = float(model(input))
        outputs.append(output)
    return outputs
output___ = eval_model(model, data)
