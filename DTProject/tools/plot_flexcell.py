# Author: Santiago Gil
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt

base_path = "data_flexcell/output/"
filename = base_path + "outputs.csv"
df_kuka = pd.read_csv("data_flexcell/input/joint_position_data.csv")
factor = 1.6884730000e9
df_kuka=df_kuka[(df_kuka["timestamp"]>32+factor) & (df_kuka["timestamp"]<43+factor)]
#df_kuka_exp['timestamp'] = pd.to_datetime(df_kuka_exp['timestamp'])
df_kuka["time"] = df_kuka['timestamp'].astype(float)
df_kuka["time"] = df_kuka["time"] - 1688473032
df_ur5e = pd.read_csv("data_flexcell/input/ur5e_actual.csv",sep=" ")
df_ur5e = df_ur5e[(df_ur5e["timestamp"]>14842) & (df_ur5e["timestamp"]<14853)]
df_ur5e["time"] = df_ur5e["timestamp"].astype(float)-14842

font = {'font.family' : 'monospace',
        'font.weight' : 'bold',
        'axes.titlesize'   : 18,
        'axes.labelsize'   : 14,
        'legend.fontsize' : 10,
        'xtick.labelsize': 10,
        'ytick.labelsize': 10,
       }

plt.rcParams.update(font)
df_cosim = pd.read_csv(filename)
df_cosim.loc[0,:] = df_cosim.loc[1,:].values
df_cosim.loc[0,["time"]] = 0.0
df_cosim.fillna("",inplace=True)



fig, axes = plt.subplots(2,2, figsize=(16,12))
plt.subplot(2,2,1)
df_cosim.plot(x = "time",y = ["{ur5e}.ur5e.actual_q0","{ur5e}.ur5e.actual_q1",
                                    "{ur5e}.ur5e.actual_q2","{ur5e}.ur5e.actual_q3",
                                   "{ur5e}.ur5e.actual_q4","{ur5e}.ur5e.actual_q5"],
             figsize=(14,10),
             title = "Co-sim UR5e joints",
             ax=axes[0,0])
plt.axvline(x=0.0,linestyle="--")
plt.axvline(x=5.0,linestyle="--")
plt.xlabel('time [s]')
plt.ylabel('radians')
plt.grid()
plt.tight_layout()

plt.subplot(2,2,2)
df_cosim.plot(x = "time",y = ["{kuka}.kuka.actual_q0","{kuka}.kuka.actual_q1",
                                    "{kuka}.kuka.actual_q2","{kuka}.kuka.actual_q3",
                                   "{kuka}.kuka.actual_q4","{kuka}.kuka.actual_q5","{kuka}.kuka.actual_q6"],
             figsize=(14,10),
             title = "Co-sim Kuka lbr iiwa 7 joints",
             ax=axes[0,1])
plt.axvline(x=0.0,linestyle="--")
plt.axvline(x=5.0,linestyle="--")
plt.xlabel('time [s]')
plt.ylabel('radians')
plt.grid()
plt.tight_layout()

plt.subplot(2,2,3)
df_ur5e.plot(x = "time",y = ["actual_q_0","actual_q_1",
                                    "actual_q_2","actual_q_3",
                                   "actual_q_4","actual_q_5"],
             figsize=(14,10),
             title = "Real UR5e joints",
             ax=axes[1,0])
plt.axvline(x=0.925,linestyle="--")
plt.axvline(x=5.525,linestyle="--")
plt.xlabel('time [s]')
plt.ylabel('radians')
plt.grid()
plt.tight_layout()

plt.subplot(2,2,4)
df_kuka.plot(x = "time",y = ["actual_q_0","actual_q_1",
                                    "actual_q_2","actual_q_3",
                                   "actual_q_4","actual_q_5","actual_q_6"],
             figsize=(14,10),
             title = "Real Kuka lbr iiwa 7 joints",
             ax=axes[1,1])
plt.axvline(x=0.599,linestyle="--")
plt.axvline(x=5.120,linestyle="--")
plt.xlabel('time [s]')
plt.ylabel('radians')
plt.grid()
plt.tight_layout()

fig.savefig(base_path + 'experiment_plot.pdf', dpi=300)
fig.savefig(base_path +'experiment_plot.png', dpi=300)
