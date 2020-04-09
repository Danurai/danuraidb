var corecount;
var _db_cards;
var _filter = {"pack_code":["Core"],"sphere_code":"leadership","type_code":["hero","ally","attachment","event"]};  //treasure, player-side-quest 'objective ally'
var _pageno = 1;
var _pages = Array(9).fill({});


corecount = (localStorage.getItem('lotrdb_corecount') || 1);
packs = localStorage.getItem('lotrdb_packs');
$('#core'+corecount).button('toggle');
if (packs != null) {
  _filter.pack_code = $.merge(["Core"],JSON.parse(packs));
  $.each(JSON.parse(packs), function (id,p) {
    $('#packs').find('input[data-code='+p+']').prop('checked',true);
  });
  $('#packs').find('.list-group-item').each(function(id,e) {
    var packcount = $(e).find('input[type=checkbox].pack:checked').length;
    $(e).find('input[type=checkbox].cycle').prop('checked',(packcount == 6));
  });
}

$.getJSON('/lotrdb/api/data/cards',function (data) {
  data = data
    .filter(c => -1 < $.inArray(c.type_code, _filter.type_code))
    .map(c => $.extend(c,{"normalname": c.name}));
  //normalise names
  _db_cards = TAFFY(data);
  
  setpacks();
});

$('#spheres').on('click','li',function () {
  _filter.sphere_code = $(this).data('code');
  setpacks();
});

$('#types').on('click','li',function () {
  _pageno = Math.max(
    1,
    ((_pages.findIndex(c => c.type_code == $(this).data('code')))/9)+1
  )
  writepage(); 
})

$('#coresets').on('change', function () {
  corecount = (parseInt($('input[type=radio]:checked', this).val()));
  localStorage.setItem('lotrdb_corecount',corecount);
  writepage();
});

$('#packs')
  .on('change','input[type=checkbox].cycle',function () {
    $(this).closest('div.list-group-item').find('input[type=checkbox].pack').prop('checked',$(this).prop('checked'));
    setpacks();
  })
  .on('change','input[type=checkbox].pack',function() {
    var packcount = $(this).closest('div.list-group-item').find('input[type=checkbox].pack:checked').length;
    $(this).closest('div.list-group-item').find('input[type=checkbox].cycle').prop('checked',(packcount == 6));
    setpacks();
  });
  
$('#pager').on('click','button',function() {
  _pageno = Math.min(
    (_pages.length / 9),
    Math.max(
      1, 
      (_pageno + parseInt($(this).val()))));
  writepage();
});
  
function setpacks() {
  var packs = $.merge(['Core'],$('#packs').find('input[type=checkbox].pack:checked').toArray().map(p=>$(p).data('code')));
  localStorage.setItem('lotrdb_packs',JSON.stringify(packs));
  _filter.pack_code = packs;
  // build folder pages
  
  _pages = [];
  var crdlst = _db_cards(_filter);
  
  $.each(["hero","ally","attachment","event"],function (id, t) {
    var tmp = crdlst.filter({"type_code":t}).order('normalname').get();
    $.merge(_pages,tmp);
    if (tmp.length%9 > 0) {$.merge(_pages,Array(9-(tmp.length%9)).fill({}));}
  });    
  
  _pageno = 1;  
  writepage();
}
 
function writepage() {
  var start = (_pageno-1) * 9;
  var cards = _pages.slice(start,start+9);
  $('#pagetype').html('<span class="text-capitalize">'+cards[0].type_code+'</span>');
  
  $('#pageno').html(_pageno + "/" + (_pages.length/9));
  
  $('#page').html(
    cards.map(c=>
      '<div class="col-4 mb-2">'
      + '<div style="position: relative">'
      +   (typeof c.quantity != 'undefined' 
            ? '<span class="py-1 px-2" style="position: absolute; right: 5px; bottom: 5px; background-color: white; opacity: 0.8; border-radius: 8px;">x' 
              + (c.pack_code == "Core" ? corecount * c.quantity : c.quantity)
              + '</span>'
            : '')
      +   (typeof c.name == 'undefined'
            ? '<img class="img-fluid rounded " src="/img/lotrdb/player_back.jpg" style="opacity: 0.3;" />'
            : '<img class="img-fluid rounded cardlink" data-toggle="modal" data-target="#cardmodal" src="' + c.cgdbimgurl + '" title="'+c.name + ' - '+c.pack_name+' #'+c.position+'" data-code="' + c.code + '" />')
      +   '</div>'
      + '</div>').join('')
   )
}
function lortdb_markdown (str) {
  var rtn;
  // Icons
  rtn = str.replace(/\[(\w+)\]/g,(($0,$1) => '<i class="lotr-type-' + $1.toLowerCase() + '"></i>'));
  return rtn;
}

$('#cardmodal').on('show.bs.modal',function (e) {
  var code = $(e.relatedTarget).data('code');
  var c = _db_cards({"code":code.toString()}).first();
  $('#cardname').html(
    '<div>' 
    + (c.is_unique == true ? '<i class="lotr-type-unique unique-icon mr-1" />' : '')
    + c.name
    + '</div>');
  $('#carddata').html(
    (typeof c.traits != 'undefined' ? '<div class="text-center"><b>' + c.traits + '</b></div>' : '')
    + '<div style="white-space: pre-wrap">' + lortdb_markdown(c.text) + '</span>'
    + (typeof c.flavor != 'undefined' ? '<div class="mt-1"><em style="white-space: pre-wrap">' + c.flavor + '</em></span>' : ''));
  $('#cardfooter').html(
    '<div class="muted">' + c.pack_name + ' #' + c.position + '</div>'
    + '<img class="icon-sm mr-2" src="/img/lotrdb/icons/sphere_' + c.sphere_code + '.png">')
  $('#cardimg').prop('src',c.cgdbimgurl);
});
