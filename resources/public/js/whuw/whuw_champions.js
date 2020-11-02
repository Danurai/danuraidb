$('#instoggle').on('change',function () {
  showinspired();
});



function showinspired() {
  show = $('#instoggle').prop('checked');
  $('.inspired').prop('hidden',!show);
  $('.normal').prop('hidden',show);
}

$(document).on('keypress', function (evt) {
  if (evt.key == "i" | evt.keycode == 105) {
    $('#instoggle').closest('label').button('toggle');
    showinspired();
  }
});

$('.champ')
  .on('mouseover', function () {
    $('#fullimg').append('<img class="img-fluid" src="' + $(this).attr('src') + '"></img');
  })
  .on('mouseout',function () {
    $('#fullimg').empty();
  });
  
$('#collectiontab').on('change', function () {
  let ownedbands = Array.from($(this).find('input:checked')).map(inp=>$(inp).data('warband_id'))
  window.localStorage.setItem('whuw_warbands_collection',JSON.stringify(ownedbands));
  updateWarbands(ownedbands);
});

let lsdata = window.localStorage.getItem('whuw_warbands_collection');

if (lsdata) {
  let owned = JSON.parse(lsdata);
  owned.forEach(function(wb,idx) {
    $('input[data-warband_id="'+wb+'"]').prop('checked',true);
  });
  updateWarbands(owned)
}

function updateWarbands(owned) {
  // Global Owned
  if (owned) {
      $('#championstab .list-group-item').prop('hidden',true);
      owned.forEach(function (wb, idx) {
        $('.list-group-item[data-warband_id="'+wb+'"]').prop('hidden',false);
      });
  }
}
