function getcgdbImageUrl ( card ) {
  return "http://www.cardgamedb.com/forums/uploads/lotr/ffg_" + cgdbCardName(card) + ".jpg";
}

function cgdbCardName (card) {
  var pack = _db_packs({"code":card.pack_code}).first();
  var cardname;
  switch (true) {
    case (pack.id < 23):
      cardname = normalizeName(card.name) + "_" + normalizeName(card.pack_name) + "_" + card.position;
        break;
    case (23 < pack.id <26):
      cardname = normalizeName(card.name) + "-" + normalizeName(card.pack_name) + "-" + card.position;
      break;
    case (pack.id < 40):
      cardname = pack.sku + "_" + right( "000" + card.position ,3);
      break;
    case (pack.id < 24):
    case (pack.id < 37):
    case (pack.id < 38):
    case (pack.id < 39):
    case (pack.id < 61):
      cardname = 
        normalizeName(card.name).toLowerCase() 
        + "-" 
        + (cgdbPackName[card.pack_code] ? cgdbPackName[card.pack_code] : card.pack_code.toLowerCase());
      break;
    default: 
      pack.sku + "_" + card.position;
  }
  return cardname;
}

function normalizeName ( name ) {
  return name
    .replace(/\s/g,"-")
    .replace(/\'|\!/,"")
    .replace(/[\u00c0-\u00c5]/,"A")
    .replace(/[\u00c8-\u00cb]/,"E")
    .replace(/[\u00cc-\u00cf]/,"I")
    .replace(/[\u00d2-\u00d6]/,"O")
    .replace(/[\u00d9-\u00dc]/,"U")
    .replace(/[\u00e0-\u00e5]/,"a")
    .replace(/[\u00e8-\u00eb]/,"e")
    .replace(/[\u00ec-\u00ef]/,"i")
    .replace(/[\u00f2-\u00f6]/,"o")
    .replace(/[\u00f9-\u00fc]/,"u");
}



// DATA
var cgdbPackName = {
  "HfG":  "thfg",
  "JtR":  "ajtr",
  "HoEM": "thoem",
  "WitW": "twitw",
  "BoG":  "tbog",
  "Starter": "core",
  "TiT": "trouble-in-tharbad",
}
var skuCode = {                      //; id (MEC)sku
  1:  1,                                // Core
  2:  2,  3:  3,  4: 4,  5: 5,  6:  6,  7:  7,   // Shadows of Mirkwood
  8:  8,                                // Khazad-dum
  9:  9,  10: 10, 11: 11, 12: 12, 13: 13, 14: 14,   // Dwarrowdelf
  15: 17,                               // Heirs of Numenor
  16: 18, 17: 19, 18: 20, 19: 21, 20: 22, 21: 23,   // Against the Shadow
  22: 25,                               // Voice of Isengard/
  23: 26, 24: 27, 25: 28, 26: 29, 27: 30, 28: 31,   // Ring-maker
  29: 38,                               // The Lost Realm
  30: 39, 31: 40, 32: 41, 33: 42, 34: 43, 35: 44,   // Angmar Awakened
  36: 47,                               // The Grey Havens
  37: 48, 40: 34, 41: 45, 42: 46, 43: 48, 44: 49 ,  // Dream-chaser
  38: 16, 39: 24,                          // Saga - The Hobbit 
  45: 50, 46: 51, 47: 54, 48: 52, 49: 53, 55: 62 ,  // Saga - The Lord of the Rings
  53: 55,                               // The Sands of Harad
  50: 56, 51: 57, 52: 58, 54: 59, 56: 60, 57: 61,   // Haradrim
  58: 65,                               // The Wilds of Rhovanion
  59: 66, 60: 67, 62: 68, 63: 69, 65: 70,         // Ered Mithrin
  61: 73,                                // 2 player Ltd Collectors Edition Starter
  64: 99 // PoD
}

/* Parse Filter *\
  
name

// NA Deck Builder d: deck ;p or e
e: pack_code      
// NA Deck Builder n: encounter_name 
r: traits         
s: sphere_code    
t: type_code     
u: is_unique 
x: text, x: "Some Text" (quotes)
y: cycle_position

\*              */

function parsefilter(f)  {
  var res;
  var outp = {};
    
  res = /e:(.+?\s|.+)/.exec(f);
  if (res != null) { 
    outp.pack_code = {"likenocase":res[1].trim().split("|")}; 
  }
  
  res = /r:(.+?\s|.+)/.exec(f);
  if (res != null) {
    outp.traits = {"likenocase":res[1].trim().split("|")};
  }
  
  res = /s:(\w+)/.exec(f);
  if (res != null) {
    outp.sphere_name = {"isnocase":res[1]};
  }
  
  res = /t:(\w+)/.exec(f);
  if (res != null) {
    outp.type_name = {"isnocase":res[1]};
  }
  
  res = /u:(true|false)/.exec(f);
  if (res != null) {
    outp.is_unique = (res[1] == "true");
  }
  
  res = /x:(\w+)/.exec(f);
  if (res != null) {
    outp.text = {"likenocase":[res[1].trim()]};
  }
  res = /x:\"(.+)\"/.exec(f);
  if (res != null) {
    outp.text = {"likenocase":[res[1].trim()]};
  }
  
  res = /y:(\d+)/.exec(f);
  if (res != null) {
    outp.cycle_position = parseInt(res[1]);
  }
  
  // Cost/threat, Attack Defense Willpower
  res = /a(\S)([0-9])/.exec(f);
  if (res != null)  {
    outp.attack = getNumberFilter(res[1],res[2]);
  }
  res = /d(\S)([0-9])/.exec(f);
  if (res != null)  {
    outp.defense = getNumberFilter(res[1],res[2]);
  }
  res = /w(\S)([0-9])/.exec(f);
  if (res != null)  {
    outp.willpower = getNumberFilter(res[1],res[2]);
  }
  res = /c(\S)([0-9])/.exec(f);
  if (res != null)  {
    outp.cost = getNumberFilter(res[1],res[2]);
  }
  
  if ($.isEmptyObject(outp) && f != "")  {
    outp.name = {likenocase:f.split('|')};
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

/*
function add_toast(msg) {
  var $toast = $('<div class="toast" role="alert" aria-live="assertive" aria-atomic="true" data-autohide="true">'
    + '<div class="toast-header"><i class="fas fa-exclamation text-primary mr-2"></i><b class="mr-auto">AoSC DB</b></div>'
    + '<div class="toast-body">'
    + msg
    + '</div></div>');
  $('#toaster').append($toast);
  $toast.toast({delay: 3000}).toast("show");
}
*/