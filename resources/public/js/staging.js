$('#selectall').on('click',function() {
  tbody = $('body').find('tbody');
  if ($(tbody).find('input[type=checkbox]:not(:checked)').length > 0) {
    $(tbody).find('input[type=checkbox]').prop('checked',true);
  } else {
    $(tbody).find('input[type=checkbox]').prop('checked',false);
  }
});

$('#deletedata').on('click',function() {
  var staged = $('body').find('tbody input[type=checkbox]:checked');
  var c = 0;
  var pb = $('#deletemodal').find('.progress-bar');
  var id = setInterval(prog_fn,10);
  
  $.each(staged,function (id, ele) {
    $.post("/staging/delete" ,{uid: $(ele).data('d').uid}, function (resp) {
      console.log(resp);
      c++;
      if (c == staged.length) { location.reload();}
    });
  });
  //$('#deletemodal').modal('hide');
  
  function prog_fn() {
    if (c == staged.length) {
      clearInterval(id);
    } else {
      var prg = parseInt(100 * (c+1) / staged.length) + '%';
      pb.css('width',prg);
      pb.html((c+1) + " of " + staged.length);
    }
  }
});

$('#importselected').on('click', function () {
  var staged = $('body').find('tbody input[type=checkbox]:checked');
  //  (POST "/save" [id name system data alliance tags notes]
  $.each(staged, function (id, ele) {
    deck = $(ele).data('d');
    if (deck.type == "deck") {
      var dx  ={
        name: deck.name,
        system: parseInt(deck.system),
        data: deck.decklist,
        notes: deck.notes,
        tags: deck.tags,
        alliance: deck.alliance
      }
      $.post("/decks/save", dx, function (r) {
        add_toast("Deck " + deck.name + " saved to your collection.")
      });
    } else if (deck.type == "collection") {
      var dx = {
        collectionjson: deck.decklist
      }
      $.post("/aosc/collection/save", dx, function (r) {
        add_toast("Collection saved");
      });
    }
  });
});

function add_toast(msg) {
  var $toast = $('<div class="toast" role="alert" aria-live="assertive" aria-atomic="true" data-autohide="true">'
    + '<div class="toast-header">'
    + '<i class="fas fa-exclamation text-primary mr-2"></i>' 
    + '<b class="mr-auto">AoSC DB</b>'
    + '</div>'
    + '<div class="toast-body">'
    + msg
    + '</div></div>');
  $('#toaster').append($toast);
  $toast.toast({delay: 3000}).toast("show");
}