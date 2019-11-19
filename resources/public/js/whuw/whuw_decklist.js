var _cards;

$.getJSON("/whuw/api/cards",function (data) {
  _cards = TAFFY(data);
});

$('#importdeck')
  .on('show.bs.modal', function() {
    $('#decksystem').val(2);
  });
$('#importdeckname').on('input',function() {
  $('#deckname').val($(this).val())
});
$('#importdecklist').on('input',function() {
  $('#deckdata').val(parse_whuw_decklist($(this).val()))
});

$('#deletemodal').on('show.bs.modal', function (ev) {
  var button = $(ev.relatedTarget);
  $(this).find('.modal-body').html('Are you sure you want to delete the decklist <b>' + button.data('name') + '</b>');
  $('#deletemodaldeckname').val(button.data('name'));
  $('#deletemodaldeckuid').val(button.data('uid'));
});


function parse_whuw_decklist ( decklist ) {
  var code = [];
  try {
    code = JSON.parse(decklist);
  } catch (e) {
    $.each(decklist.match(/.+/g),((id, c) =>
      code.push(_cards({"name":c}).first().code)
    ));
  }
  return JSON.stringify(code.filter(c=>c!=null));
}
  


/* COMMON */

$('#exportdeck').on('show.bs.modal',function (ev) { 
  $(this).find('.modal-header>span').html($(ev.relatedTarget).data('deckname'));
  $(this).find('textarea').val($(ev.relatedTarget).data('export'))
  $(this).find('input').val(parse_whuw_decklist($(ev.relatedTarget).data('export')));
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

$('li [data-toggle=collapse]').on('click',function() {
  $(this).find('button[data-toggle=collapse] [data-fa-i2svg]').toggleClass('fa-plus fa-minus');
});