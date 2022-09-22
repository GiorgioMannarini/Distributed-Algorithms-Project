import argparse
import os, atexit
import textwrap
import time
import tempfile
import threading, subprocess
import random
import signal
import random
import time
from enum import Enum

from collections import defaultdict, OrderedDict

class Validation:
    def __init__(self, processes, messages, outputDir):
        self.processes = processes
        self.messages = messages
        self.outputDirPath = os.path.abspath(outputDir)
        if not os.path.isdir(self.outputDirPath):
            raise Exception("`{}` is not a directory".format(self.outputDirPath))

    def generateConfig(self):
        # Implement on the derived classes
        pass

    def checkProcess(self, pid):
        # Implement on the derived classes
        pass

    def checkAll(self, continueOnError=True):
        ok = True
        for pid in range(1, self.processes+1):
            ret = self.checkProcess(pid)
            if not ret:
                ok = False

            if not ret and not continueOnError:
                return False

        return ok

class LCausalBroadcastValidation(Validation):
    def __init__(self, processes, messages, outputDir, causalRelationships):
        super().__init__(processes, messages, outputDir)
        self.vectorClocksPerProcess = {}
        self.deliveriesPerProcess = {}
        self.causalRelationships = causalRelationships
        for i in range(1, processes + 1):
            self.deliveriesPerProcess[i] = []


        print(self.causalRelationships)
    def generateConfig(self):
        hosts = tempfile.NamedTemporaryFile(mode='w')
        config = tempfile.NamedTemporaryFile(mode='w')

        for i in range(1, self.processes + 1):
            hosts.write("{} localhost {}\n".format(i, PROCESSES_BASE_IP+i))

        hosts.flush()

        config.write("{}\n".format(self.messages))
        for i in range(1, self.processes + 1):
            relationships = self.causalRelationships[i]
            print(relationships)
            config.write("{}\n".format(' '.join(str(dep) for dep in relationships)))
        config.flush()

        return (hosts, config)
    def checkProcess(self, pid):
        filePath = os.path.join(self.outputDirPath, f'{pid}.output')

        i = 1
        nextMessage = defaultdict(lambda : 1)
        filename = os.path.basename(filePath)
        nLines = 0
        tmpVectorClock = [0 for p in range(int(self.processes))]
        vectorClocks = {}


        with open(filePath) as f:
            for lineNumber, line in enumerate(f):
                tokens = line.split()

                # Check broadcast
                if tokens[0] == 'b':
                    msg = int(tokens[1])
                    if msg != i:
                        print("File {}, Line {}: Messages broadcast out of order. Expected message {} but broadcast message {}".format(filename, lineNumber, i, msg))
                        return False

                    vc = tmpVectorClock.copy()
                    vc[pid-1] = i-1
                    vectorClocks['{} {}'.format(pid, msg)] = list(vc)
                    i += 1

                nLines += 1
                # Check delivery
                if tokens[0] == 'd':
                    sender = int(tokens[1])
                    msg = int(tokens[2])

                    if msg != nextMessage[sender]:
                        print("File {}, Line {}: Message delivered out of order. Expected message {}, but delivered message {}".format(filename, lineNumber, nextMessage[sender], msg))
                        return False
                    if sender == pid and i <= msg:
                        print("File {}, Line {}: Message delivered before the broadcast. Last broadcast was message {}, but delivered message {}".format(filename, lineNumber, i - 1, msg))

                    else:
                        nextMessage[sender] = msg + 1
                    #map the message to the current vector clock
                    if sender != pid:
                        vectorClocks['{} {}'.format(sender, msg)] = list(tmpVectorClock)
                    #update process vector clock
                    tmpVectorClock[sender - 1] += 1

                    self.deliveriesPerProcess[pid].append({'sender': sender, 'msg': msg})

        if nLines != self.messages * (self.processes + 1):
            print("P{}: Found {} messages instead of {}".format(pid, nLines, self.messages * (self.processes + 1)))
        #print(vectorClocks)
        self.vectorClocksPerProcess[pid] = vectorClocks
        return True

    def checkCausality(self):
        for pid, deliveries in self.deliveriesPerProcess.items():
            for delivery in deliveries:
                senderId = delivery['sender']
                msg = delivery['msg']
                senderClock = self.vectorClocksPerProcess[senderId]['{} {}'.format(senderId, msg)]
                pClock =  self.vectorClocksPerProcess[pid]['{} {}'.format(senderId, msg)]
                for idx, (p, s) in enumerate(zip(pClock, senderClock)):
                    if idx + 1 in self.causalRelationships[senderId] and  p < s:
                        print('Inconsistency found for delivery {} at pid {}. \nProcess clock: {}  \nsender clock: {}'.format(delivery, pid, pClock, senderClock))
                        print('Index {}: {} < {}'.format(idx + 1, p, s))
                        print('Sender dependencies {}'.format(self.causalRelationships[senderId]))
                        return False
        return True
    def checkAll(self, continueOnError=True):
        cond1 = super(LCausalBroadcastValidation, self).checkAll(continueOnError)
        return cond1 and self.checkCausality()


if __name__ == '__main__':
    tests = [{
        'outputDir': '../example/output',
        'processes': 3,
        'messages': 200000,
        'dependencies': {
            1: [1],
            2: [2, 1],
            3: [3, 1, 2],
        }
    }]

    for test in tests:
        print(test)
        validation = LCausalBroadcastValidation(test['processes'], test['messages'], test['outputDir'],test['dependencies'])
        assert(validation.checkAll() == True)