[0,1,0,0,0]+[0,0,0,0,1]

~item = [\rs247567456,[1,1,0,0,1]];
~item[0]

~dict = Dictionary.new
~dict.put(\21,~item)
~dict.put(\21,~item++[[1,0,0,0,1]])
~dict.at(\21)[1l]

SNPInfo.baseToVec(\AA)
SNPInfo.lengthAtOneMs

~dictionary.
