Computer Networks Project 3: Data Link Layer Simulation

This program simulates sending a file across hosts on the data link layer.
Both the sender and receiver run on the local machine, so the program can
optionally simulate packet loss or data corruption, as would be common on an
actual network transmission.

-----------

COMPILING:

javac DLLSim.java

RUNNING:

java DLLSim <errType> <srcFile> <outFile>

errType must be 0, 1, or 2.
    0: The file will be transmitted as-is.
    1: The second packet will be dropped.
    2: Part of the first packet will be corrupted.

scrFile must exist and must be accessible by the program.

outFile will be placed in the program's working directory.

-----------

IMPLEMENTATION NOTES:

Before the data is transmitted, the sender creates an initial packet, where
the first byte of the data contains the number of packets to follow.  This
way, when the receiver reads this packet, it can allocate the appropriate
amount of storage space, and will know when the sender has finished
transmitting the file.

If the program is set to simulate packet corruption, the first data byte of
the first packet will be replaced with a random byte value from 0 to 255.

Once the transmission finishes, the program will print a statement indicating
that the data has been sent, and that the results can be seen in the output
and log files.
