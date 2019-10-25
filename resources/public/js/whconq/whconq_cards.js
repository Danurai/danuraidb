var _cards = TAFFY();

$(document).ready(function () {
  $.getJSON('/whconq/api/cards', function (data)  {
    _cards = TAFFY(data);
  });
});