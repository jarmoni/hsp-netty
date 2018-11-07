[![Build Status](https://api.travis-ci.org/jarmoni/hsp-netty.svg?branch=master)](https://travis-ci.org/jarmoni/hsp-netty)
# hsp-netty
Provides a Java-implementation of the [HSP](https://github.com/jarmoni/hsp-spec)-codec for usage in netty-based servers/clients.

## Requirements
- Java 8
- Java >8 not supported atm.

## Known limitations
- No support for 128-Bit messageId's. Upper limit is 32 Bit (should be suffiecient for most use-cases)
- Decoder uses Netty's [ReplayingDecoder](https://github.com/netty/netty/blob/4.1/codec/src/main/java/io/netty/handler/codec/ReplayingDecoder.java) which is a convenient but (with regard to performance) maybe not the best solution.
