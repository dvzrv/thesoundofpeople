SNPResolver{
	var <fileEnd = ".json";
	var <idList = "";
	var <userID;
	var <thisUser;
	var <users;
	var <usersWithGenotypes = 0;
	var <file;
	var <path;
	var <>resolver;
	//initialize with a path to write json files to (/tmp for example), with a minimum free disk space of ~17-25GB!
	*new{
		arg aPath = "/tmp/theSoundOfPeople/", aID = 1;
		^super.new.init(aPath, aID);
	}

	init{
		arg aPath, aID;
		path = aPath;
		userID = aID.asInt;
		if(File.exists(path++userID++"/").not, {
			protect{
				File.mkdir(path++userID++"/");
			}{
				("Oh noes. The directory '"++this.path++"' could not be created. Check if all parent dirs are there and you have write permission!").warn;
			};
		});
//		this.downloadUserList;
	}

	downloadUserList{//download a new users.json and parse it for users with genotypes
		var usersFinished = Condition.new(false);
		var phenoFinished = Condition.new(false);
		fork{
			("Downloading user list to: "++path++userID.asString++"/"++"users"++fileEnd).postln;
			("curl -s -o "++path++userID.asString++"/"++"users"++fileEnd++" http://opensnp.org/users"++fileEnd).unixCmd({
				arg res,pid;
				if(res.asInt!=0,{
					"Server down. Try again later!".postln
				},{//if the download finished, resume with parsing
					usersFinished.unhang;
				});
			}, false);//save to file with user id as parentdir
			usersFinished.hang;
			"Extracting users with genotypes...".postln;
			users = (path++userID.asString++"/"++"users"++fileEnd).asString.parseYAMLFile;
			users.do({//retrieve the user IDs of users that uploaded genotypes
				arg item, i;
				if(users[i].at("genotypes")[0].notNil,{
					if(users[i].at("id").asInt!=userID, {//exclude the user we're currently working on
						idList = idList ++ users[i].at("id").asString++",";
						usersWithGenotypes = usersWithGenotypes + 1;
					});
				});
			});
			idList.removeAt(idList.size-1);//remove the last ","
			"Done".postln;
			("Downloading user phenotype information to: "++path++userID.asString++"/"++userID.asString++".phenotypes"++fileEnd).postln;
			("curl -s -o "++path++userID.asString++"/"++userID.asString++".phenotypes"++fileEnd++" http://opensnp.org/phenotypes/json/"++userID.asString++fileEnd.asString).unixCmd({
				arg res,pid;
				if(res.asInt!=0,{
					"Server down. Try again later!".postln
				},{//if the download finished, resume with parsing
					phenoFinished.unhang;
				});
			}, false);//save to file with user id as parentdir
			phenoFinished.hang;
			thisUser = (path++userID.asString++"/"++userID.asString++".phenotypes"++fileEnd).asString.parseYAMLFile;
			("Finished downloading phenotypes of "++thisUser.at("user").at("name").asString++".").postln;
		}
	}

	downloadResolver{//get a resolver for an ID (of a base pair at a certain position) from opensnp.org
		arg id;
		var state = false; //, downloadFinished = Condition.new(false);
		if(File.exists(path++userID.asString++"/"++id.asString++fileEnd).not,{
			("Downloading JSON file for "++id).postln;
			("curl -s -o "++path++userID.asString++"/"++id.asString++fileEnd++" http://opensnp.org/snps/"++id.asString++fileEnd).unixCmd({
				arg res,pid;
				if(res.asInt!=0,{
					"Failed! Retry later.".postln;
				},{
					"Success!".postln;
				});
			},false);//save to file with user id as parent directory
			state = true;
		},{
			state = true;
		});
		^state;
	}
	
	retrieveResolver{//parse a downloaded JSON file and retrieve a resolver for a given base (pair) with an ID. Returns resolver base or \none
		arg snp, id;
		var baseCase = 0;
		if(File.exists(path++userID.asString++"/"++id.asString++fileEnd),{//if the json file exists, parse it and return the resolver (if found)
			var matchHunt = 0, huntOver = false, jsonReturn, resolver, jsonFile;
			jsonFile = (path++userID.asString++"/"++id.asString++fileEnd);
			try{
				jsonReturn = jsonFile.parseYAMLFile;//parse the JSON file
				while({ matchHunt < size(jsonReturn) && huntOver.not },{//iterate the parsed JSON for a combo match 
					if(jsonReturn[matchHunt].includesKey("error").not,{//if error, skip this one
						if(size(jsonReturn[matchHunt].at("user").at("genotypes"))!=0,{//if there is no data, skip this one
							if(jsonReturn[matchHunt].at("user").at("genotypes")[0].at("local_genotype").asString != "--",{//if it's invalid (too), skip this one
								if(SNPInfo.isBase(snp),{//check if single base or base pair
									baseCase = 1;
								});
								switch(baseCase,
									0,{//base pair lookup
										if(jsonReturn[matchHunt].at("user").at("genotypes")[0].at("local_genotype").asString != snp.asString && SNPInfo.isBasePair(jsonReturn[matchHunt].at("user").at("genotypes")[0].at("local_genotype")),{//check if it's member of base pair and not the same base pair
											("SUCCESS: Different base found for "++id.asString++" in ("++(matchHunt+1).asString++"/"++jsonReturn.size.asString++")!").post;
											(snp.asString++" -> "++jsonReturn[matchHunt].at("user").at("genotypes")[0].at("local_genotype").asString).postln;
											resolver = this.calcResolverPair(snp, jsonReturn[matchHunt].at("user").at("genotypes")[0].at("local_genotype").asSymbol);
											huntOver = true;
										});
									},
									1,{//single base lookup
										if(jsonReturn[matchHunt].at("user").at("genotypes")[0].at("local_genotype").asString != snp.asString && SNPInfo.isBase(jsonReturn[matchHunt].at("user").at("genotypes")[0].at("local_genotype")),{//check if it's member of base pair and not the same base pair
											("SUCCESS: Different base found for "++id.asString++" in ("++(matchHunt+1).asString++"/"++jsonReturn.size.asString++")!").post;
											(snp.asString++" -> "++jsonReturn[matchHunt].at("user").at("genotypes")[0].at("local_genotype").asString).postln;
											resolver = [jsonReturn[matchHunt].at("user").at("genotypes")[0].at("local_genotype").asSymbol];
											huntOver = true;
										});
									}
								);
							});
						});
					});
					matchHunt = matchHunt + 1;
				});
				jsonReturn = nil;
				if(resolver.isNil,{//if no resolver was found return none
					switch(baseCase,
						0,{^SNPInfo.emptyBasePair},
						1,{^SNPInfo.emptyBase}
					);
				},{
					^resolver;
				});
			}{
				("FAILED: Parsing not possible, file corrupt: "++id.asString++fileEnd).postln;
				("Skipping for now and removing.").postln;
				File.delete(path++userID.asString++"/"++id.asString++fileEnd);
			}
		},{//file is not there yet, download it? something like that...
			("FAILED: Resolver file for "++id.asString++" not available yet. Download it.").postln;
//			this.downloadResolver(id);	
		});
	}

	calcResolverPair{//calculate upper/lower ends of resolver pairs
		arg base, resolver;
		switch(base,
			\AA,{
				switch(resolver,
					\AC,{^[resolver,\CC]},
					\CC,{^[\AC,resolver]},
					\AG,{^[resolver,\GG]},
					\GG,{^[\AG,resolver]},
					\AT,{^[resolver,\TT]},
					\TT,{^[\AT,resolver]}
					);
			},
			\CC,{
				switch(resolver,
					\AA,{^[resolver,\AC]},
					\AC,{^[\AA,resolver]},
					\CG,{^[resolver,\GG]},
					\GG,{^[\CG,resolver]},
					\CT,{^[resolver,\TT]},
					\TT,{^[\CT,resolver]}
				);
			},
			\GG,{
				switch(resolver,
					\AA,{^[resolver,\AG]},
					\AG,{^[\AA,resolver]},
					\CG,{^[\CC,resolver]},
					\CC,{^[resolver,\CG]},
					\GT,{^[resolver,\TT]},
					\TT,{^[\GT,resolver]}
				);
			},
			\TT,{
				switch(resolver,
					\AA,{^[resolver,\AT]},
					\AT,{^[\AA,resolver]},
					\CT,{^[\CC,resolver]},
					\CC,{^[resolver,\CT]},
					\GT,{^[\GG,resolver]},
					\GG,{^[resolver,\GT]}
				);
			}
		);
	}
}
