$(document).ready(function () {
  (function() {
    'use strict';
    window.addEventListener('load', function() {
      // Fetch all the forms we want to apply custom Bootstrap validation styles to
      var forms = document.getElementsByClassName('needs-validation');
      // Loop over them and prevent submission
      var validation = Array.prototype.filter.call(forms, function(form) {
        form.addEventListener('submit', function(event) {
          if (form.checkValidity() === false) {
            event.preventDefault();
            event.stopPropagation();
          }
          form.classList.add('was-validated');
        }, false);
      });
    }, false);
  })();
  
  function pwd_score_hr (score) {
    var str = '';
    switch (score) {
      case 0:
        str='<span class="text-danger">Very Weak</span>';
        break;
      case 1:
        str='<span class="text-secondary">Weak</span>';
        break;
      case 2:
        str='<span class="text-warning">OK</span>';
        break;
      case 3:
        str='<span class="text-success">Strong</span>';
        break;
      case 04:
        str='<span class="text-info">Very Strong</span>';
        break;
    }
    return str;
  }
  
  $('#username').on('input',function () {
    $.post("/checkusername", {username: $(this).val()})
      .done(function (data) {
        var ele = $('#username').get(0);
        if (data == "true") {
          ele.setCustomValidity("Username Already Exists");
          $(ele.closest('div')).find('.invalid-feedback').html("Username Already Exists");
        } else {
          ele.setCustomValidity("");
          $(ele.closest('div')).find('.invalid-feedback').html("Username Required");
        }
      });
  });
  
  $('#password').on('input', function () {
    var check = zxcvbn($(this).val());
    $('#pwdscoremeter').val(check.score + 1);
    $('#pwdscoretxt').html(pwd_score_hr (check.score));
  });
  
  $('#password,#password1').on('input', function () {
    var ele = $('#password1').get(0);
    if ($('#password').val() != $('#password1').val()) {
      ele.setCustomValidity("Passwords must match");
    } else {
      ele.setCustomValidity("");
    }
  });
});