/* Shared Functions */


/* Parse Filter */
//e: setcode
//f: faction
//t: Type
//x: Text (one word)
//x: "Some Text" (quotes)
//u: true/false (unique)

function parsefilter(f)	{
	var res;
	var outp = {};
	
  var txt = /x:(.+)/;
  res = RegExp(txt).exec(f);
  if (res != null) {
    outp["text"] = {likenocase:res[1]};
  }
  
	var set = /e:(\S+)/;
	res = RegExp(set).exec(f)
	if (res !== null)	{
		outp["pack_code"] = {likenocase:res[1].split('|')};
	}
	
	var faction = /f:(\S+)/;
	res = RegExp(faction).exec(f)
	if (res !== null)	{
		outp["faction_code"] = {likenocase:res[1].split('|')};
	}
  
  var cost = /r(\S)([0-9])/;
	res = RegExp(cost).exec(f);
	if (res != null)	{
    outp["cost"] = getNumberFilter(res[1],res[2]);
	}
  
  var command_icons = /c(\S)([0-9])/;
	res = RegExp(command_icons).exec(f);
	if (res != null)	{
    outp["command_icons"] = getNumberFilter(res[1],res[2]);
	}
  
  var cost = /a(\S)([0-9])/;
	res = RegExp(cost).exec(f);
	if (res != null)	{
    outp["attack"] = getNumberFilter(res[1],res[2]);
	}
  
  var cost = /h(\S)([0-9])/;
	res = RegExp(cost).exec(f);
	if (res != null)	{
    outp["hp"] = getNumberFilter(res[1],res[2]);
	}
  
  var cost = /c(\S)([0-9])/;
	res = RegExp(cost).exec(f);
	if (res != null)	{
    outp["cost"] = getNumberFilter(res[1],res[2]);
	}
  
	
	var unique = /u:(true|false)/;
	res = RegExp(unique).exec(f)
	if (res != null)	{
		outp["unique"] = res[1] == "true";
	}
	
	var loyal = /l:(true|false)/;
	res = RegExp(loyal).exec(f)
	if (res != null)	{
		outp["signature_loyal"] = (res[1] == "true" ? "Loyal" : {"!=":"Loyal"});
	}
	
	if ($.isEmptyObject(outp) && f != "")	{
		outp["name"] = {likenocase:f.split('|')};
	}
  
	return outp;
}

function getNumberFilter(op,val)  {
  var value = parseInt(val,10);
  var numberFilter = '';
  switch (op) {
    case ':':
      numberFilter = value;
      break;
    case '>':
      numberFilter = {">":value};
      break;
    case '<':
      numberFilter  = {"<":value};
      break;
    case '!':
      numberFilter  = {"!=":value};
      break;
  }
  return numberFilter;
}

/* Fisher-Yates Shuffle  */
function shuffle(array) {
	var m = array.length, t, i;

	// While there remain elements to shuffle…
	while (m) {

		// Pick a remaining element…
		i = Math.floor(Math.random() * m--);

		// And swap it with the current element.
		t = array[m];
		array[m] = array[i];
		array[i] = t;
	}

	return array;
}