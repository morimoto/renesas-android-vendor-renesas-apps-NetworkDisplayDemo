#!/usr/bin/env python

from threading import Thread
from subprocess import Popen, PIPE, STDOUT
import select, socket
import SocketServer

HOST = ''
REC_NAME = 'TestReceiver'
PORT = 53515
IP = '172.22.89.45'

bufferSize = 1024
meta_data = '{"port":%d,"name":"TestReceiver @ %s","id":"%s","width":1280,"height":960,"mirror":"h264","audio":"pcm","subtitles":"text/vtt","proxyHeaders":true,"hls":false,"upsell":true}' % (PORT, REC_NAME, IP)

SAVE_TO_FILE = False


class MyTCPHandler(SocketServer.BaseRequestHandler):
    def handle(self):
        p = Popen(['gst-launch-1.0','-vvv', 'fdsrc', "do-timestamp=true", '!', 'h264parse','config-interval=1', 'disable-passthrough=true', '!', 'omxh264dec', 'no-reorder=true', '!', 'waylandsink'], stdin=PIPE, stdout=PIPE)
        skiped_metadata = False
        while True:
            data = self.request.recv(bufferSize)
            if data == None or len(data) <= 0:
                break
            if not skiped_metadata:
                print "Client connected, addr: ", self.client_address[0]
                if data.find('\r\n\r\n') > 0:
                    last_ctrl = data.find('\r\n\r\n') + 4
                    print 'Recv control data: ', data[0:last_ctrl]
                    if len(data) > last_ctrl:
                        p.stdin.write(data[last_ctrl:])
                skiped_metadata = True
            else:
                #print "write data[", len(data), "]"
                p.stdin.write(data)
        p.kill()


def resp_hello(ip, port):
    send_sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    send_sock.sendto(meta_data, (ip, port))


def handle_discovery():
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    s.bind(('', PORT))
    s.setblocking(0)
    while True:
        result = select.select([s],[],[])
        if len(result[0]) <= 0:
            continue
        msg, address = result[0][0].recvfrom(bufferSize)
        print 'Receive broadcast msg: ', msg
        if msg == 'hello':
            print 'Got discover msg, src ip: %s, port: %d' % (address[0], address[1])
            resp_hello(address[0], address[1])


if __name__ == "__main__":
    server = SocketServer.TCPServer((HOST, PORT), MyTCPHandler)
    server_thread = Thread(target=server.serve_forever)
    server_thread.daemon = True
    server_thread.start()

    handle_discovery()
    server.shutdown()
