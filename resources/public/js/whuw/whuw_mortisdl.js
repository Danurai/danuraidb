$('.deletedl').on('click',function (ev) {
  ev.stopPropagation();
  $('#deletemodal').find('.modal-body').html('Are you sure you want to delete the decklist<br /><b>' + $(this).data('name') + '</b>');
  $('#deletemodaldeckname').val($(this).data('name'));
  $('#deletemodaldeckuid').val($(this).data('uid'));
  $('#deletemodal').modal('show');
});