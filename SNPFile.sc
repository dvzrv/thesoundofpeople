SNPFile{
	var <snpDict;
	var <file;
	var <fileLength;
	var <userID;
	var <testSet = inf;
	*new{
		arg aFile, aTestSet = inf;
		^super.new.init(aFile, aTestSet);
	}

	init{
		arg aFile, aTestSet;
		this.setFileAndUser(aFile);
		testSet = aTestSet;
	}

	
	setFileAndUser{//set the file (again) to be parsed
		arg aFile;
		var allSlash, allDot;
		file = aFile;
		allSlash = file.findAll("/");
		allDot = file.findAll(".");
		if(allSlash.notNil,{//if there is no slash in the filename
			userID = file[allSlash[allSlash.size-1]+1..allDot[allDot.size-2]-1].asInt;
		},{
			userID = file[0..allDot[allDot.size-2]-1].asInt;
		});
	}

	readFile{//read a SNPDict from file
		var snpFile = File(file, "r"), line = "", counter = 0, procCounter = Array.new(9), tmp, key, sizeOf, snp, res, keyCount = 0.0, unknown, unknownCount = 0.0;
		fileLength = (("wc -l "++file.shellQuote).unixCmdGetStdOut).delimit({|ch| ch.isSpace});
		9.do({|item,i| procCounter.add(round((fileLength[0].asFloat/100)*(i+1)*10, 1).asInt)});//calculating procentuals for a simple progression output
		("Now reading: "++file++" ("++fileLength[0]++" lines).").postln;
		("==========").postln;
		if(snpFile.isOpen,{
			protect{
				while{(line = snpFile.getLine).notNil }{
					switch(line[0].asString, 
						"*",{//info line
							sizeOf = line.delimit({|ch| ch.isSpace});//delimit by space and/or tab
							snpDict = SNPDict.new(sizeOf[1].asFloat, sizeOf[3].asInt);//init new SNPDict
							snpDict.initLookupFromFile(sizeOf[1].asFloat, sizeOf[2].asFloat);
						},
						";",{//key line
							key = line.delimit({|ch| ch.isSpace});//delimit by space and/or tab
							snpDict.addPositionLookupFromFile(keyCount, key[1].asFloat);//store to lookup table!
							keyCount = keyCount + 1.0;
						},
						"+",{//SNP line
							tmp = line.delimit({|ch| ch.isSpace});//delimit by space and/or tab
							if(tmp[6].isNil,{
								res = [tmp[5].asSymbol];
							},{
								res = [tmp[5].asSymbol, tmp[6].asSymbol];
							});
							snp = SNP.new(tmp[1], tmp[2], tmp[3], tmp[4], res);
							snpDict.storeSNP(snp, key[1]);
						},
						"|",{//unknown line
							unknown = line.delimit({|ch| ch.isSpace});//delimit by space and/or tab
							snpDict.addUnknownLookupFromFile(unknownCount, unknown[1].asFloat);
							unknownCount = unknownCount + 1.0;
						}
					);
					counter = counter + 1;
					switch(counter,
						procCounter[0],{"=".post;},
						procCounter[1],{"=".post;},
						procCounter[2],{"=".post;},
						procCounter[3],{"=".post;},
						procCounter[4],{"=".post;},
						procCounter[5],{"=".post;},
						procCounter[6],{"=".post;},
						procCounter[7],{"=".post;},
						procCounter[8],{"=".post;}
					);
				};
			"=".postln;
			"Done reading file to RAM.".postln;
			}{
				snpFile.close;
			};
		},{
			("Couldn't open file for reading: "++file).warn;
		});
		^snpDict;
	}

	writeFile{//write a SNPDict to file
		arg snpDict;
		var snpFile = File(file, "w"), line = "", counter = 0, tmp, snp, newSameCounter = 0;
		("Attempting to write SNPDict to file: "++file).postln;
		if(snpFile.isOpen, {
			protect{
				snpFile.write("*\t"++snpDict.snpArr.size.asString++"\t"++snpDict.unknownArr.size.asString++"\t"++snpDict.userID++"\n");//write length of snpArr and unknownArr
				snpDict.snpArr.do({
					arg item;
					snpFile.write(";\t"++item.asString++"\n");
					snpDict.snpAtPosition(item).do({//write all SNPs
						arg snp;
						var res;
						if(snp.resolver.size==2,{
							res = (snp.resolver[0]++"\t"++snp.resolver[1]).asString;
						},{
							res = snp.resolver[0].asString;
						});
						snpFile.write("+\t"++snp.chromosome.asString++"\t"++snp.position.asString++"\t"++snp.id.asString++"\t"++snp.base.asString++"\t"++res++"\n");//write SNP to file 
					});
				});
				snpDict.unknownArr.do({//write all unknown positions
					arg item;
					snpFile.write("|\t"++item.asString++"\n");
				});
			}{
				snpFile.close;
				("Done writing to file.").postln;
				^true;
			};
		},{
			("Couldn't open file for writing: "++file).postln;
			^false;
		});
	}

	writeUnknownIDToFile{//write the rsids of all unknown SNPs to file
		arg snpDict;
		var snpFile = File("/media/Data/tmp/1/1.unknown.snp", "w");
		("Attempting to write unknowns to file: "++file).postln;
		if(snpFile.isOpen, {
			protect{
				snpDict.unknownArr.do({
					arg item,i;
					snpDict.noneResolverAtPosition(item).do({
						arg snp,i;
						snpFile.write(snp.id++"\n");
					});
				});
			}
		},{
			("Couldn't open file for writing: "++file).postln;
			^false;
		});
	}

	readUnknownIDFromFile{
		arg unknowns;
		var snpFile = File(unknowns, "r"), fileLength, line ="", outArr, counter = 0;
		fileLength = (("wc -l "++unknowns.shellQuote).unixCmdGetStdOut).delimit({|ch| ch.isSpace});
		fileLength.postln;
		outArr = Array.new(fileLength[0].asInt);
		if(snpFile.isOpen, {
			protect{
				while{(line = snpFile.getLine).notNil }{
					outArr.add(line.asSymbol);
					counter = counter + 1;
				}
			}
			^outArr;
		},{
			("Couldn't open file for writing: "++file).postln;
			^false;
		});
	}
}
