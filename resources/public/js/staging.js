$('#selectall').on('click',function() {
  tbody = $('body').find('tbody');
  if ($(tbody).find('input[type=checkbox]:not(:checked)').length > 0) {
    $(tbody).find('input[type=checkbox]').prop('checked',true);
  } else {
    $(tbody).find('input[type=checkbox]').prop('checked',false);
  }
});