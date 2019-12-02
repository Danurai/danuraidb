var _cards;

$.getJSON('/aosc/api/data/cards',function (data) {
  var source_data = data['hits']['hits'].map(c => c['_source']);
  _cards = TAFFY(
    source_data.map(card => $.extend({"setnumber": card.set[0].number.toString(),"setname": card.set[0].name,"categoryname": card.category.en},card)));
});

$('#importdeck').on('show.bs.modal', function(ev) {
  $('#decksystem').val(1);
});

//global

$('#deletemodal').on('show.bs.modal', function (ev) {
  var button = $(ev.relatedTarget);
  $(this).find('.modal-body').html('Are you sure you want to delete the decklist <b>' + button.data('name') + '</b>');
  $('#deletemodaldeckname').val(button.data('name'));
  $('#deletemodaldeckuid').val(button.data('uid'));
});

$('#exportdeck').on('show.bs.modal',function (ev) {
  $(this).find('.modal-header>span').html($(ev.relatedTarget).data('deckname'));
  $(this).find('textarea').val($(ev.relatedTarget).data('export'))
  $(this).find('input').val(parse_deck_list($(ev.relatedTarget).data('export')));
});
  
$('#importdeckname').on('input',function() {
  $('#deckname').val($(this).val());
});

$('#exportdeck').on('click','.input-group', function () {
  $(this).find('input').select();
  document.execCommand("copy");
  add_toast($('#exportdeck').find('.modal-header>span').html() + " Deck Sharing Code copied to clipboard");
});
//

$('#importdecklist').on('input',function () {
  var sharingcode;
  if (parse($(this).val()).version == 1) {
      sharingcode = $(this).val()
  } else {
    sharingcode = parse_deck_list ($(this).val());
  }
  $('#deckdata').val(sharingcode);
});

var staging_uri = "https://danuraidb.herokuapp.com/staging"

$('.btn-stage').on('click',function () {
  var d = $(this).data('d')  
  var dx = {
    name: d.name,
    system: parseInt(d.system),
    decklist: d.data,
    type: "deck"
  }
  $.post("https://danuraidb.herokuapp.com/staging",dx,function(x){
    console.log(x);
    add_toast("Deck " + dx.name + " staged");
  });
});

//////////////////////////////////
  
function parse_deck_list (data) {
  var crd;
  var deck = [];
  
  var regex = /([1-3])x\s((.+)\s\((.+)\)|(.+))/g;
  var res = data.match(regex);
  
  $.each(res, function (id, item) {
    item.match(regex);
    
    var qty = parseInt(RegExp.$1, 10);
    var cname =  RegExp.$2;
    
    crd = _cards({"name":cname}).first();
    
    deck.push({id: crd.id, count: qty});
  });
  return generate(deck);
}


$('#exportall').on('click',function() {
  var $temp = $('<input>');
  var deckjson = JSON.stringify($(this).data('export'));
  $('body').append($temp);
  $temp.val(deckjson);
  $temp.select();
  document.execCommand("copy");
  document.execCommand("paste");
  add_toast($temp.val() == deckjson ? "All decklists copied to clipboard as JSON" : "decklist copy failed");
  $temp.remove();
});

$('#importallsubmit').on('click',function(ev) {
  var decks = JSON.parse($('#importalldata').val()).reverse();
  var saved = 0;
  var id = setInterval(prog_fn,10);
  var pb = $('#importallmodal').find('.progress-bar');
  
  $('#importallmodal').find('button').addClass('disabled');
  $('#importallmodal').find('button').attr('disabled',true);
  $.each(decks, function(id,d) {
    $.post('/decks/import',$.extend(d,{"system": 1}), function() {
      saved++
      if (saved == decks.length) { location.reload();}
    });
  });
  function prog_fn() {
    if (saved == decks.length) {
      clearInterval(id);
    } else {
      var prg = parseInt(100 * (saved+1) / decks.length) + '%';
      pb.css('width',prg);
      pb.html((saved+1) + " of " + decks.length);
    }
  }
});
