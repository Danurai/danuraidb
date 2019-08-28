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
  
function add_toast(msg) {
  var $toast = $('<div class="toast" role="alert" aria-live="assertive" aria-atomic="true" data-autohide="true">'
    + '<div class="toast-header"><b class="mr-auto">AoSC DB</b></div>'
    + '<div class="toast-body">'
    + msg
    + '</div></div>');
  $('#toaster').append($toast);
  $toast.toast({delay: 3000}).toast("show");
}
  
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