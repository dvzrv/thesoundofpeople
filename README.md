thesoundofpeople
================

Supercollider classes for interacting with data sets from http://opensnp.org (parsing, sonification, etc.).


This is a prototype! It's quite slow and eats a lot of RAM. I've only tested it on Linux with JACK. So be aware of that, when using it!


So far, what you can do with it: 
- Parse 23andme files, write them to a different format.
- Loading both of those files into a dictionary.//this will eat so much RAM, cookie monster is a 'lil puppet compared (get ready for 5,7Gb (parsing) and 4,7Gb (reading)
- Playing positions (or rather: bases/base pairs on a chromosome at a position) of up to 25 chromosomes from those dictionaries on 2,4,6 & 8 channels (there's no real limit to that though, but your hardware)


There are some upcoming TODOs, and I'll list some of them:
- flesh out SNP.sc (a million instances eat too much RAM on filling a SNPDict), that comes with a lot of looking around what has to be changed in the other classes to make this work.
- fine grain the synthesizers used, so each chromosome will become more distinct (currently only X, Y and MT are very recognizable)
- use a set of filters based upon the form of each chromosome and its "interesting areas"
- make updating resolvers less RAM consuming (and thus usable)
