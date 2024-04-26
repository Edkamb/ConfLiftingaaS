#!/usr/bin/env python3
from robots_flexcell import robots
import paho.mqtt.client as mqtt
import socket
import numpy as np
from pathlib import Path

import pika
import json
from datetime import datetime
import time
from threading import Timer

from pyhocon import ConfigFactory
import logging

step_size = 0.2
max_time = 11.0

logging.disable(logging.CRITICAL)


ur_robot_model = robots.UR5e_RL()
kuka_robot_model = robots.KukaLBR_RL()

rabbitmq_host = "localhost"
rabbitmq_port = 5672
rabbitmq_username = "guest"
rabbitmq_password = "guest"

mqtt_client = mqtt.Client()
mqtt_host = '127.0.0.1'
mqtt_port = 1883
mqtt_username = ""
mqtt_password = ""

conf_ur5e = ConfigFactory.parse_file('config_files_flexcell/ur5e_actual.conf')
ur5e_base_topic = conf_ur5e.get_string('mqtt.topic')

conf_kuka = ConfigFactory.parse_file('config_files_flexcell/kuka_actual.conf')
kuka_base_topic = conf_kuka.get_string('mqtt.topic')

#### Robots ####
def compute_ur_q(X,Y,Z):
    comp_x,comp_y,comp_z = ur_robot_model.compute_xyz_flexcell(X,Y,Z=Z)
    y_rot,z_rot = ur_robot_model.compute_yz_joint(comp_y,comp_z)
    target_position = ur_robot_model.compute_q(comp_x,y_rot,z_rot)[0]
    return target_position

def compute_kuka_q(X,Y,Z):
    comp_x,comp_y,comp_z = kuka_robot_model.compute_xyz_flexcell(X,Y,Z=Z)
    target_position = kuka_robot_model.compute_q(comp_x,comp_y,comp_z)[0]
    return target_position


def transmit_robot_motion(q,robot):
    if (robot == "UR5e"):
        mqtt_client.publish(ur5e_base_topic + "movej",str(q).replace("[","").replace("]",""))
        #for j in range(len(joint_pos)):
            #queue_server.send_string_ur(f"actual_q_{j} {joint_pos[j]}")
        #    pass
    elif (robot == "Kuka"):
        mqtt_client.publish(kuka_base_topic + "moveptprad",str(q).replace("\n","").replace("[","").replace("]",""))
            #q_degrees = np.degrees(np.array(q))
            #kuka_robot.move_ptp_rad(q=q_degrees)
        #for j in range(len(joint_pos)):
            #queue_server.send_string_kuka(f"actual_q_{j} {joint_pos[j]}")
        #    pass


#### RabbitMQ ####
credentials = pika.PlainCredentials(rabbitmq_username, rabbitmq_password)
#connection = pika.BlockingConnection(pika.ConnectionParameters(rabbitmq_host, rabbitmq_port, rabbitmq_vhost, credentials=credentials))
connection = pika.BlockingConnection(pika.ConnectionParameters(rabbitmq_host, rabbitmq_port, credentials=credentials))
channel = connection.channel()
print("Declaring exchange")
channel.exchange_declare(exchange='fmi_digital_twin', exchange_type='direct')

print("Creating queue")
result = channel.queue_declare(queue='', exclusive=True)
queue_name = result.method.queue

channel.queue_bind(exchange='fmi_digital_twin', queue=queue_name,
                   routing_key='data.from_cosim')

#pub = rospy.Publisher('/Input', Int32, queue_size=10)
#rospy.init_node('talker', anonymous=True)
#rate = rospy.Rate(10) # 10hz
number = 1

def talker(number):
    consoleMsg = "About to publish %d" % number
    #rospy.loginfo(consoleMsg)
    #pub.publish(number)
    #rate.sleep()

def publish():
#    channel.stop_consuming()
    dt=datetime.strptime('2019-01-04T16:41:24+0200', "%Y-%m-%dT%H:%M:%S%z")


    target_X_ur5e = 6
    target_Y_ur5e = 16
    target_Z_ur5e = 1
    target_X_kuka = 8
    target_Y_kuka = 11
    target_Z_kuka = 2
    msg = {}
    msg['time']= dt.isoformat()
    msg['target_X_ur5e'] = target_X_ur5e
    msg['target_Y_ur5e'] = target_Y_ur5e
    msg['target_Z_ur5e'] = target_Z_ur5e
    msg['target_X_kuka'] = target_X_kuka
    msg['target_Y_kuka'] = target_Y_kuka
    msg['target_Z_kuka'] = target_Z_kuka
    msg['command_move_ur5e'] = True
    msg['command_move_kuka'] = True
    msg['motion_time_ur5e'] = 1.0
    msg['motion_time_kuka'] = 1.0
    #q_ur5e = compute_ur_q(target_X_ur5e,target_Y_ur5e,target_Z_ur5e)
    #q_kuka = compute_kuka_q(target_X_kuka,target_Y_kuka,target_Z_kuka)
    #Timer(0.05, transmit_robot_motion, args=(q_ur5e,"UR5e",)).start()
    #Timer(0.05, transmit_robot_motion, args=(q_kuka,"Kuka",)).start()
    #transmit_robot_motion(q_ur5e,"UR5e")
    #transmit_robot_motion(q_kuka,"Kuka")

    for i in range(int(max_time/step_size) + 5):
            msg['time']= datetime.now(tz = datetime.now().astimezone().tzinfo).isoformat(timespec='milliseconds')
            #msg['time']= dt.isoformat()
            print("i:" + str(i))

            if (i==int(25.0/step_size)):
                target_X_ur5e = 5
                target_Y_ur5e = 14
                target_Z_ur5e = 2
                target_X_kuka = 3
                target_Y_kuka = 10
                target_Z_kuka = 1
                msg['target_X_ur5e'] = target_X_ur5e
                msg['target_Y_ur5e'] = target_Y_ur5e
                msg['target_Z_ur5e'] = target_Z_ur5e
                msg['target_X_kuka'] = target_X_kuka
                msg['target_Y_kuka'] = target_Y_kuka
                msg['target_Z_kuka'] = target_Z_kuka
                msg['motion_time_ur5e'] = 1.0
                msg['motion_time_kuka'] = 1.0
                #q_ur5e = compute_ur_q(target_X_ur5e,target_Y_ur5e,target_Z_ur5e)
                #q_kuka = compute_kuka_q(target_X_kuka,target_Y_kuka,target_Z_kuka)
                #Timer(0.05, transmit_robot_motion, args=(q_ur5e,"UR5e",)).start()
                #Timer(0.05, transmit_robot_motion, args=(q_kuka,"Kuka",)).start()
                #transmit_robot_motion(q_ur5e,"UR5e")
                #transmit_robot_motion(q_kuka,"Kuka")

            if (i==int(4.0/step_size)):
                target_X_ur5e = 5
                target_Y_ur5e = 22
                target_Z_ur5e = 0
                target_X_kuka = 7
                target_Y_kuka = 14
                target_Z_kuka = 1
                msg['target_X_ur5e'] = target_X_ur5e
                msg['target_Y_ur5e'] = target_Y_ur5e
                msg['target_Z_ur5e'] = target_Z_ur5e
                msg['target_X_kuka'] = target_X_kuka
                msg['target_Y_kuka'] = target_Y_kuka
                msg['target_Z_kuka'] = target_Z_kuka
                msg['motion_time_ur5e'] = 1.0
                msg['motion_time_kuka'] = 1.0
                #q_ur5e = compute_ur_q(target_X_ur5e,target_Y_ur5e,target_Z_ur5e)
                #q_kuka = compute_kuka_q(target_X_kuka,target_Y_kuka,target_Z_kuka)
                #Timer(0.05, transmit_robot_motion, args=(q_ur5e,"UR5e",)).start()
                #Timer(0.05, transmit_robot_motion, args=(q_kuka,"Kuka",)).start()
                #transmit_robot_motion(q_ur5e,"UR5e")
                #transmit_robot_motion(q_kuka,"Kuka")

            if(i==int(23.4/step_size)):
                target_X_ur5e = 0
                target_Y_ur5e = 20
                target_Z_ur5e = 3
                target_X_kuka = 6
                target_Y_kuka = 6
                target_Z_kuka = 3
                msg['target_X_ur5e'] = target_X_ur5e
                msg['target_Y_ur5e'] = target_Y_ur5e
                msg['target_Z_ur5e'] = target_Z_ur5e
                msg['target_X_kuka'] = target_X_kuka
                msg['target_Y_kuka'] = target_Y_kuka
                msg['target_Z_kuka'] = target_Z_kuka
                msg['motion_time_ur5e'] = 1.0
                msg['motion_time_kuka'] = 1.0
                #q_ur5e = compute_ur_q(target_X_ur5e,target_Y_ur5e,target_Z_ur5e)
                #q_kuka = compute_kuka_q(target_X_kuka,target_Y_kuka,target_Z_kuka)
                #Timer(0.05, transmit_robot_motion, args=(q_ur5e,"UR5e",)).start()
                #Timer(0.05, transmit_robot_motion, args=(q_kuka,"Kuka",)).start()
                #transmit_robot_motion(q_ur5e,"UR5e")
                #transmit_robot_motion(q_kuka,"Kuka")


            channel.basic_publish(exchange='fmi_digital_twin',
                          routing_key='data.to_cosim',
                          body=json.dumps(msg))
            print(" [x] Sent %s" % json.dumps(msg))


            time.sleep(step_size)

def callback(ch, method, properties, body):
    print(" [x] %r" % body)
    if "waiting for input data for simulation" in str(body):
      publish()
    else:
        print("Received: %s", str(body))
        number = 1
        talker(number)

if __name__ == '__main__':
    try:
        # MQTT
        mqtt_client.username_pw_set(username=mqtt_username, password=mqtt_password)
        mqtt_client.connect(mqtt_host, mqtt_port, 60)

        # RabbitMQ
        channel.basic_consume(
                    queue=queue_name, on_message_callback=callback, auto_ack=True)

        channel.start_consuming()

        connection.close()
    except KeyboardInterrupt:
        #ur5e_robot.stop_recording()
        pass
