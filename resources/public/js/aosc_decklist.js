var _cards;

$.getJSON('/aosc/api/data/cards',function (data) {
  var source_data = data['hits']['hits'].map(c => c['_source']);
  _cards = TAFFY(
    source_data.map(card => $.extend({"setnumber": card.set[0].number.toString(),"setname": card.set[0].name,"categoryname": card.category.en},card)));
});

$('#deletemodal').on('show.bs.modal',function (ev) {
  var button = $(ev.relatedTarget);
  $(this).find('.modal-body').html ("Are you sure you want to delete " + button.data("name"));
  $(this).find('input').val(button.data("uid"));
});

$('#exportdeck').on('show.bs.modal',function (ev) {
  $(this).find('.modal-header>span').html($(ev.relatedTarget).data('deckname'));
  $(this).find('textarea').val($(ev.relatedTarget).data('export'))
  $(this).find('input').val(parse_deck_list($(ev.relatedTarget).data('export')));
});
  
$('#importdeckname').on('input',function() {
  $('#deckname').val($(this).val());
});

$('#importdecklist').on('input',function () {
  var sharingcode;
  if (parse($(this).val()).version == 1) {
      sharingcode = $(this).val()
  } else {
    sharingcode = parse_deck_list ($(this).val());
  }
  $('#deckcode').val(sharingcode);
});


$('#exportdeck').on('click','.input-group', function () {
  $(this).find('input').select();
  document.execCommand("copy");
  add_toast($('#exportdeck').find('.modal-header>span').html() + " Deck Sharing Code copied to clipboard");
});
  
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
    $.post('/aosc/decks/import',d, function() {
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