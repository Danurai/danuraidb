var _db_cards;

$.get('/lotrdb/api/data/cards',function (data) {
  _db_cards = TAFFY(data.filter(c=>c.pack_code!="Starter"));
});

function normalise (name) {
  return name
    .replace(/[\u00c0-\u00c5]/g, "A")
    .replace(/[\u00c8-\u00cb]/g, "E")
    .replace(/[\u00cc-\u00cf]/g, "I")
    .replace(/[\u00d2-\u00d6]/g, "O")
    .replace(/[\u00d9-\u00dc]/g, "U")
    .replace(/[\u00e0-\u00e5]/g, "a")
    .replace(/[\u00e8-\u00eb]/g, "e")
    .replace(/[\u00ec-\u00ef]/g, "i")
    .replace(/[\u00f2-\u00f6]/g, "o")
    .replace(/[\u00f9-\u00fc]/g, "u")
    .replace(/[!\'\"]/g,"")
    .replace(/\s\-/g,"-")
    .toLowerCase()
}
var pack_transform = {
  "hfg": "thfg",
  "jtr": "ajtr",
  "hoem": "thoem",
  "witw": "twitw",
  "bog": "tbog"
}

function getimgurl(c) {
  //var pc = pack_transform[c.pack_code.toLowerCase()]||c.pack_code.toLowerCase();
  //return "http://cardgamedb.com/forums/uploads/lotr/" + n_name + "-" + pc + ".jpg";
  
  return c.cgdbimgurl; //.replace('ffg_','');
}


$('#types').on('change',function() {
  var tc = $($(this).find('input:checked')).data('type_code');
  var outp = '';
  $('#cards').empty();
  _db_cards({"type_code":tc}).each(function (c) {
    var n_name = normalise(c.name);
    var imgurl = getimgurl(c);
    outp = '<div class="col-sm-2 card" data-code="' + c.code + '" data-toggle="modal" data-target="#cardmodal">'
      +'<img class="img-fluid" src="' + imgurl + '" title="' + n_name +'" />'
      +'<div>'+c.name+'</div><div>'
      +'</div>';
      
    $('#cards').append(outp);
  });
});

$('#cardmodal').on('show.bs.modal',function (ev) {
  var code = $(ev.relatedTarget).data('code').toString();
  var crd = _db_cards({"code": code}).first();
  $('#cardname').html(crd.name);
  $('#cardimg').html('<img src="'+getimgurl(crd)+'">');
});