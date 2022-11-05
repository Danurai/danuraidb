var corecount;
var _db_cards;
var _filter = {"pack_code":["Core"],"sphere_code":"leadership","deck":"Hero","type_code":["hero","ally","attachment","event","player-side-quest",]};  //treasure, player-side-quest 'objective' 'contract'
var _pageno = 1;
var _pages = Array(9).fill({});

document.addEventListener('touchstart', handleTouchStart, false);        
document.addEventListener('touchmove', handleTouchMove, false);
var xDown = null;                                                        
//var yDown = null;  

function handleTouchStart(evt) {                                         
    xDown = evt.touches[0].clientX;                                      
    //yDown = evt.touches[0].clientY;                                      
}; 

function turnpage ( val ) {
 _pageno = Math.min(
    (_pages.length / 9),
    Math.max(
      1, 
      (_pageno + parseInt(val))));
  writepage();
}

function handleTouchMove(evt) {
    if ( ! xDown ) { //|| ! yDown ) {
        return;
    }
    var xUp = evt.touches[0].clientX;                                    
    //var yUp = evt.touches[0].clientY;
    var xDiff = xDown - xUp;
    //var yDiff = yDown - yUp;

    if (Math.abs(xDiff) > 150) {
    //if ( Math.abs( xDiff ) > Math.abs( yDiff ) ) {/*most significant*/
        turnpage( xDiff > 0 ? 1 : -1)
      /* reset values */
      xDown = null;
    //  yDown = null;         
    } 
    //else {
    //    if ( yDiff > 0 ) {
    //    /* up swipe */ 
    //    } else { 
    //    /* down swipe */
    //    }                                                                 
    //}                                    
};


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
    var checkcount = $(e).find('input[type=checkbox].pack').length;
    $(e).find('input[type=checkbox].cycle').prop('checked',(packcount == checkcount));
  });
}

$.getJSON('/lotrdb/api/data/cards',function (data) {
  _db_cards = TAFFY(data
                      .filter(c => _filter.type_code.includes(c.type_code))
                      .filter(c => c.type_code != 'hero' | ['baggins','fellowship'].includes(c.sphere_code)==false  ) );
  setpacks();
});

$('#spheres').on('click','li',function () {
  core_spheres = ['leadership','lore','spirit','tactics']
  extra_spheres = ['neutral','baggins','fellowship']
  sphere = $(this).data('code')
  _filter.sphere_code = core_spheres.includes(sphere) ? sphere : extra_spheres  
  setpacks();
});

$('#types').on('click','li',function () {
  _pageno = Math.max(
    1,
    ((_pages.findIndex(c => c.type_code == $(this).data('code')))/9)+1
  )
  writepage(); 
})

$('#coresets').on('input', function () {
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
    var packcount = $(this).closest('div.list-group-item').find('input[type=checkbox].pack').length;
    var checkcount = $(this).closest('div.list-group-item').find('input[type=checkbox].pack:checked').length;
    $(this).closest('div.list-group-item').find('input[type=checkbox].cycle').prop('checked',(packcount == checkcount));
    setpacks();
  });
  
$('#pager').on('click','button',function() {
  turnpage(parseInt($(this).val()));
});

$('.selectpacks').on('click', function(e) {
  e.preventDefault();
  let all = $(this).data('select') == 'all';
  $('#packs').find('input[type="checkbox"]').prop('checked',all)
  setpacks();
});
  
function setpacks() {
  var packs = $.merge(['Core'],$('#packs').find('input[type=checkbox].pack:checked').toArray().map(p=>$(p).data('code')));
  localStorage.setItem('lotrdb_packs',JSON.stringify(packs));
  _filter.pack_code = packs;
  // build folder pages
  
  _pages = [];
  var crdlst = _db_cards(_filter);
  
  $.each(_filter.type_code,function (id, t) {
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
  updateCardCounts();
}


function updateCardCounts() {
  let total_cards = 0;
  let total_unique = 0;
  
  $('#cardcounts').empty();
  _filter.pack_code.forEach( fp => {
    let pack_cards = _db_cards({"pack_code":fp})
    let pack_unique = pack_cards.count()
    let pack_total = pack_cards.sum("quantity");
    if (fp == 'Core') {
      pack_total *= corecount = $('#coresets').find('input[type=radio]:checked').val();
    }
    total_unique += pack_unique
    total_cards  += pack_total;
    $('#cardcounts').append(`<div class="d-flex"><div>${_db_cards({"pack_code":fp}).first().pack_name}</div><div class="ml-auto">${pack_unique} / ${pack_total}</div></div>`) 
  });
  $('#cardcounts').append(`<div class="d-flex"><b>Total</b><b class="ml-auto">${total_unique} / ${total_cards}</b></div>`)
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
