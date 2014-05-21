SNPGUI {

  var <speakerSetup;//Array holding the chromosome numbers for each speaker
  var <mainView;// main view object
  var <drawView;// main view object
  var <speakerDict;//Dictionary of Views (per speaker)
  var <textDict;//Dictionary of StaticText obbjects
  var <taskSNP;//task for updating the SNP texts of chromosomes
  var <snpDict; //Dictionary of texts for SNPs on chromosomes (replaceable)
  var <arcRadius = 200;//size of the arc, surrounded by speakers
  var <radiusOffset = 75;//size of the arc, surrounded by speakers
  var <mainWidth = 800;
  var <mainHeight = 800;
  var <speakerHeight = 50;
  var <speakerWidth = 50;
  var <lineHeight = 12;

  *new{
    arg setup;
    ^super.new.snpGUIInit(setup);
  }

  snpGUIInit{
    arg setup;
    setup.size.postln;
    speakerSetup = setup;
    this.setupViews(setup.size);
    //create a dictionary the size of the setup
  }
  // initialize Dictionaries for all things
  initDicts{
    arg setup;
    speakerDict = Dictionary.new(setup.size+1);
    //textDict = Dictionary.new(setup.size);
    textDict = Dictionary.new(25);//Dictionary holding all StaticText objects that can be modified
    snpDict = Dictionary.new(25);
    //taskDict = Dictionary.new(25);
    //add each speaker number
    setup.do({|speaker,i|
      var tmp = Dictionary.new(setup[0].size);
      // Adds a View with speaker information to the speakerDict
      speakerDict.add((i+1).asSymbol -> this.initSpeakerView(i, setup.size, setup[0].size));
      // iterate all chromosome information from original setup
      speaker.do({|chromosome, j|
        //add chromosome number -> replaceable SNP StaticText object to temporary Dictionary
        //tmp.add(chromosome.asSymbol -> this.initChromosomeText(chromosome.asSymbol, (i+1).asSymbol, j+1));
        textDict.add(chromosome.asSymbol -> this.initChromosomeText(chromosome.asSymbol, (i+1).asSymbol, j+1));
        snpDict.add(chromosome.asSymbol -> "");
      });
      //textDict.add((i+1).asSymbol -> tmp);
    });
    // Adds a View for MT chromosome with speaker information to the speakerDict
    speakerDict.add(\all -> this.initSpeakerView(-1, setup.size, 1));
    // add chromosome text for MT to the speaker called all
    textDict.add(\25 -> this.initChromosomeText(\25, \all, 1));
    //
    snpDict.add(25.asSymbol -> "");
    //start a Task to update the SNP currently playing
    this.startSNPTask;
    //snpDict.postln;
    //speakerDict.postln;
    //textDict.postln;
  }

  //init a StaticText for a chromosome in its speaker View, returns its replaceable component (for SNPs)
  initChromosomeText{
    arg chromosome, number, position;
    var snpText, chromosomeText;
    chromosomeText = StaticText.new(speakerDict.at(number), Rect(0, lineHeight*position, (speakerDict.at(number).bounds.width/2), lineHeight));
    chromosomeText.string = (chromosome.asString++": ");
    chromosomeText.align = \left;
    snpText = StaticText.new(speakerDict.at(number), Rect((speakerDict.at(number).bounds.width/2), lineHeight*position, (speakerDict.at(number).bounds.width/2), lineHeight));
    snpText.string = "Test";
    snpText.align = \right;
    ^snpText;
  }

  //inits a View for one speaker, adds its name as StaticText object and returns itself
  initSpeakerView{
    arg number, total, perSpeaker;
    var mid, radius, radianOffset, radian, x, y, speakerContainer, text;
    mid = Point.new(mainWidth/2,mainHeight/2);//mid of the circle
    radius = arcRadius+radiusOffset;//make a slightly bigger radius for the text objects
    radianOffset = 360/total*2;// radian offset used to make speaker 1 be positioned front left
    radian = 360/total*number;//segment of the circle the speaker is placed in
    //Calculating x and y coordinates for the View to be created
    x = mid.x+(radius*cos(degrad(radian-radianOffset)));
    y = mid.y+(radius*sin(degrad(radian-radianOffset)));
    //setting up the View container for a speaker
    speakerContainer = View.new(drawView, Rect(x, y, speakerWidth, (perSpeaker+1)*lineHeight));
    speakerContainer.visible = true;
    speakerContainer.background = Color.green;

    // adding the speaker number
    text = StaticText(speakerContainer, Rect(0, 0, speakerContainer.bounds.width, lineHeight));
    text.string = (number+1).asString;
    text.align = \center;
    //add exception for no/ all speaker
    if(number<0,{
      //recalculate radius and offset
      radius = radius+radiusOffset;
      radian = 360/total*0;
      //Calculating x and y coordinates for the View to be created
      x = mid.x+(radius*cos(degrad(radian-radianOffset)));
      y = mid.y+(radius*sin(degrad(radian-radianOffset)));
      speakerContainer.moveTo(x, y);
      text.string = "all";
    });
//    ("Speaker: "++number.asString).postln;
//    ("cos("++radian.asString++"): "++cos(degrad(radian)).asString).postln;
//    ("sin("++radian.asString++"): "++sin(degrad(radian)).asString).postln;
    ^speakerContainer;
  }

  /*
  * Setup the initial window and all of its StaticText elements
  */
  setupViews{
    arg numChannels;
    mainView = View.new(nil,Rect(50,50,mainWidth,mainHeight));
    //mainView.front;
    mainView.fixedSize=mainWidth@mainHeight;
    mainView.enabled = true;
    mainView.userCanClose = true;
    mainView.alwaysOnTop;
    mainView.visible = true;
    mainView.name = "The Sound Of People";
    mainView.background = Color.white;
    mainView.bounds.postln;
    //make a UserView for drawing with Pen
    drawView = UserView.new(mainView, Rect(0, 0, mainView.bounds.width, mainView.bounds.height));
    drawView.animate = true;
    drawView.frameRate = 60.0;
    // draw a circle
    drawView.drawFunc = {
      Pen.translate(drawView.bounds.width/2, drawView.bounds.height/2);
      Pen.strokeColor = Color.black;
      Pen.color = Color.black;
      Pen.addArc(0@0, arcRadius, 0, 2pi);
      Pen.stroke;
    };
    //add StaticText objects for names of speakers, names of chromosomes and names of SNPs
    this.initDicts(speakerSetup);
  }
  //Starts a task to update texts for SNP of chromosomes on AppClock
  startSNPTask{
    taskSNP = Task.new({
      {
        //get all values from the dictionary and update the corresponding StaticText objects
        snpDict.keysValuesDo({|key, value|
          textDict.at(key.asSymbol).string_(value.asString);
        });
        0.02.wait;
      }.loop;
    });
    taskSNP.play(AppClock);
  }

  //set the text of a specific chromosome (aka.: which SNP is currently playing?)
  setSNPText{
    arg chromosome, text;
    //delete all strings first
//    snpDict.keysValuesChange{|key,value|
//      ^"";
//    };
    //add the current SNP text to the dictionary
    snpDict.put(chromosome.asSymbol, text.asString);
  }
  // get the current SNP text of a chromosome
  getSNPText{
    arg chromosome;
    var text = snpDict.at(chromosome.asSymbol);
    ^text;
  }
}
