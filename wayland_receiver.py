#!/usr/bin/env python3

from threading import Thread
from subprocess import Popen, PIPE, STDOUT
import select, socket
import SocketServer

HOST = ''
REC_NAME = 'TestReceiver'
PORT = 53515
IP = '172.22.89.45'

bufferSize = 1024
meta_data = '{\
"port":%d,\
"name":"TestReceiver @ %s",\
"id":"%s",\
"width":1280,"height":960,\
"mirror":"h264",\
"audio":"pcm",\
"subtitles":"text/vtt",\
"proxyHeaders":true,\
"hls":false,\
"upsell":true\
}' % (PORT, REC_NAME, IP)

SAVE_TO_FILE = False


class MyTCPHandler(SocketServer.BaseRequestHandler):
    def handle(self):
        p = Popen([
            'gst-launch-1.0',
            '-vvv',
            'fdsrc',
            'do-timestamp=true',
            '!',
            'h264parse',
            'config-interval=1',
            'disable-passthrough=true',
            '!',
            'omxh264dec',
            'no-reorder=true',
            '!',
            'fpsdisplaysink',
            'sync=false'
        ], stdin=PIPE, stdout=PIPE)
        skiped_metadata = False
        while True:
            data = self.request.recv(bufferSize)
            if data == None or len(data) <= 0:
                break
            if not skiped_metadata:
                print('Client connected, addr: ', self.client_address[0])
                found_pos = data.find(b'\r\n\r\n')
                if found_pos > 0:
                    last_ctrl = found_pos + 4
                    print('Recv control data: ', data[0:last_ctrl])
                    if len(data) > last_ctrl:
                        p.stdin.write(data[last_ctrl:])
                skiped_metadata = True
            else:
                #print("Write data (%d b)" % len(data))
                p.stdin.write(data)
        p.kill()


def resp_hello(addr):
    send_sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    send_sock.sendto(meta_data.encode(), addr)


def handle_discovery():
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    s.bind(('', PORT))
    s.setblocking(0)
    while True:
        result = select.select([s],[],[])
        if len(result[0]) <= 0:
            continue
        msg, address = result[0][0].recvfrom(bufferSize)
        print( 'Receive broadcast msg: ', msg)
        print(address)
        if msg == b'hello':
            print( 'Got discover msg, src ip: %s, port: %d' % address)
            resp_hello(address)


if __name__ == "__main__":
    server = SocketServer.TCPServer((HOST, PORT), MyTCPHandler)
    server_thread = Thread(target=server.serve_forever)
    server_thread.daemon = True
    server_thread.start()

    handle_discovery()
    server.shutdown()
