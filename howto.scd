//First off: All of this eats shitloads of RAM (still), so be aware! Don't use this with less than 8Gb of RAM available! You have been warned! Also make sure your /tmp has enough space if you're recording to file!


//Parsing a file, writing a new one to disk
(
//parse a new file from opensnp.org
~parser = SNPParser.new("/tmp/1.23andme.9");
//get a SNPDict
~dictionary = ~parser.readFile(100);
~dictionary = ~parser.readFile();
~parser.linesToRead
//write the SNPDict to file
~file = SNPFile("/tmp/1.sonificate.snp");
~file.writeFile(~dictionary);
)


~something = ~dictionary.storeSNP([ 1140718, \rs35222290, [ 0, 0, 0, 1, 1 ] ], 1140718)
~something = ~dictionary.snpDict.at(1140718).add([ 1140718, \rs35222290, [ 0, 0, 0, 1, 1 ] ] )
~dictionary.snpDict.at(1011521)
~dictionary.snpDict.size
~dictionary.snpDict.at(\1011521).add(\0->1)
~dictionary.snpDict.values
~dictionary.snpDict.keys

//You might want to recompile the Class library now. Your RAM will be full...
//boot server FIRST, then load file
(
s.makeWindow;
s.boot;
~file = SNPFile("/tmp/1.sonificate.snp");//read from own file (smaller)
~dictionary = ~file.readFile;//read in the file
~dictionary.positions;//the number of positions you'll be able to play
)


//Make some sound: These are examples on how to init the SNPSonificator class (only use one of them of course). Don't set the playTime below 1 hour. Your sound server won't like that... seriously!

~sonification = SNPSonificator.new(~dictionary, 8, 2, false, [], true, "/tmp/");// 8 chan, 2hour playtime, recording to file
~sonification = SNPSonificator.new(~dictionary, 8, 6, true, [], false);//8 chan, 6hour playtime
~sonification = SNPSonificator.new(~dictionary, 2, 2, true, [], true, "/var/run/media/dave/whitey/");//2 chan, 2hour playtime, recording to file
~sonification = SNPSonificator.new(~dictionary, 2, 2, true, [], false);//2 chan, 2hour playtime
~sonification = SNPSonificator.new(~dictionary, 8, 2, true, [\1,\2,\3,\4,\5,\6,\7,\8,\9,\10,\11,\12,\13,\14,\15,\16,\17,\18,\19,\20,\21,\22], false);//8 chan, 2hour playtime, ignoring all chromosomes but X, Y and MT


~sonification.playFromTo(0, 200000);//play from position - to position (round about 950k in all, if you leave second argument blank, it'll try to play to the end)
~sonification.queryTimeAndPosition;//get current position and time
~sonification.pausePlay;//pause play at current position
~sonification.resumePlay;//resume it



