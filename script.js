document.addEventListener('DOMContentLoaded', function() {

  var CONTACT_EMAIL = 'jordan.gandrey@curacasa-hin.ch';

  // ── ANNÉES D'EXPÉRIENCE (calculées automatiquement depuis le 01.08.2016) ──
  (function() {
    var start = new Date(2016, 7, 1);
    var now = new Date();
    var years = now.getFullYear() - start.getFullYear();
    var anniversaryPassed = (now.getMonth() > start.getMonth()) ||
      (now.getMonth() === start.getMonth() && now.getDate() >= start.getDate());
    if (!anniversaryPassed) years -= 1;
    document.querySelectorAll('[data-years-exp="true"]').forEach(function(el) {
      el.setAttribute('data-target', years);
    });
  })();

  // ── ANNÉE DE COPYRIGHT (automatique) ──
  var copyrightYear = document.getElementById('copyright-year');
  if (copyrightYear) copyrightYear.textContent = new Date().getFullYear();

  // ── NAV SCROLL ──
  var navbar = document.getElementById('navbar');
  if (navbar) {
    window.addEventListener('scroll', function() {
      navbar.classList.toggle('scrolled', window.scrollY > 40);
    });
  }

  // ── HAMBURGER ──
  var hamburger = document.getElementById('hamburger');
  var navLinks = document.getElementById('nav-links');
  if (hamburger && navLinks) {
    hamburger.addEventListener('click', function() {
      var open = hamburger.classList.toggle('open');
      navLinks.classList.toggle('open');
      hamburger.setAttribute('aria-expanded', open ? 'true' : 'false');
    });
  }

  // ── DISPONIBILITÉ (heure de Suisse, quel que soit le fuseau du visiteur) ──
  function getZurichDayAndHour() {
    try {
      var parts = new Intl.DateTimeFormat('en-GB', {
        timeZone: 'Europe/Zurich', weekday: 'short', hour: 'numeric', hour12: false
      }).formatToParts(new Date());
      var weekday = parts.find(function(p) { return p.type === 'weekday'; }).value;
      var h = parseInt(parts.find(function(p) { return p.type === 'hour'; }).value, 10);
      var dayIndex = ['Sun','Mon','Tue','Wed','Thu','Fri','Sat'].indexOf(weekday);
      if (dayIndex === -1 || isNaN(h)) throw new Error('parse');
      return {dayIndex: dayIndex, hour: h};
    } catch (err) {
      // Repli sur l'heure locale de l'appareil si le fuseau Europe/Zurich n'est pas supporté
      var now = new Date();
      return {dayIndex: now.getDay(), hour: now.getHours()};
    }
  }
  function updateAvailability() {
    var zurich = getZurichDayAndHour();
    var dayIndex = zurich.dayIndex, h = zurich.hour;
    var available = dayIndex >= 1 && dayIndex <= 5 && h >= 8 && h < 18;
    var badge = document.getElementById('nav-badge');
    var availText = document.getElementById('avail-text');
    var availSub = document.getElementById('avail-sub');
    if (badge) {
      badge.textContent = available ? 'Disponible' : 'Absent';
      badge.style.background = available ? 'rgba(107,143,120,.12)' : 'rgba(192,120,90,.10)';
      badge.style.color = available ? 'var(--moss)' : 'var(--clay)';
    }
    if (availText) availText.textContent = available ? "Disponible aujourd'hui" : 'Absent pour le moment';
    if (availSub) availSub.textContent = available ? 'Réponse sous 30 minutes' : 'De retour Lun–Ven, 8h–18h';
  }
  updateAvailability();
  setInterval(updateAvailability, 60000);

  // ── REVEAL ──
  var revealObserver = new IntersectionObserver(function(entries) {
    entries.forEach(function(e) { if (e.isIntersecting) e.target.classList.add('visible'); });
  }, {threshold: 0.12});
  function initReveal() {
    document.querySelectorAll('.reveal,.reveal-left,.reveal-right').forEach(function(el) { revealObserver.observe(el); });
  }

  // ── COUNTERS ──
  var counterObserver = new IntersectionObserver(function(entries) {
    entries.forEach(function(e) {
      if (e.isIntersecting && !e.target.dataset.done) {
        e.target.dataset.done = '1';
        var target = parseInt(e.target.dataset.target);
        var cur = 0; var step = Math.max(1, Math.ceil(target/40));
        var t = setInterval(function() {
          cur = Math.min(cur + step, target);
          e.target.textContent = cur;
          if (cur >= target) clearInterval(t);
        }, 40);
      }
    });
  }, {threshold: 0.3});
  function initCounters() {
    document.querySelectorAll('.counter[data-target]').forEach(function(el) { counterObserver.observe(el); });
  }

  // ── FAQ ──
  document.querySelectorAll('.faq-question').forEach(function(btn) {
    btn.addEventListener('click', function() {
      var item = btn.closest('.faq-item');
      var wasOpen = item.classList.contains('open');
      document.querySelectorAll('.faq-item').forEach(function(i) {
        i.classList.remove('open');
        i.querySelector('.faq-question').setAttribute('aria-expanded', 'false');
      });
      if (!wasOpen) { item.classList.add('open'); btn.setAttribute('aria-expanded', 'true'); }
    });
  });

  // ── SIDE PANEL ──
  var sidePanel = document.getElementById('side-panel');
  var sideOverlay = document.getElementById('side-overlay');
  function openSidePanel() {
    if (sidePanel) sidePanel.classList.add('open');
    if (sideOverlay) sideOverlay.classList.add('open');
    document.body.style.overflow = 'hidden';
  }
  function closeSidePanel() {
    if (sidePanel) sidePanel.classList.remove('open');
    if (sideOverlay) sideOverlay.classList.remove('open');
    document.body.style.overflow = '';
  }
  var btnSideTrigger = document.getElementById('btn-side-trigger');
  var btnSideClose = document.getElementById('btn-side-close');
  if (btnSideTrigger) btnSideTrigger.addEventListener('click', openSidePanel);
  if (btnSideClose) btnSideClose.addEventListener('click', closeSidePanel);
  if (sideOverlay) sideOverlay.addEventListener('click', closeSidePanel);

  var stabPatient = document.getElementById('stab-patient');
  var stabPro = document.getElementById('stab-pro');
  var sideFormPatient = document.getElementById('side-form-patient');
  var sideFormPro = document.getElementById('side-form-pro');
  if (stabPatient && stabPro && sideFormPatient && sideFormPro) {
    stabPatient.addEventListener('click', function() {
      stabPatient.classList.add('active');
      stabPro.classList.remove('active');
      sideFormPatient.hidden = false;
      sideFormPro.hidden = true;
    });
    stabPro.addEventListener('click', function() {
      stabPro.classList.add('active');
      stabPatient.classList.remove('active');
      sideFormPatient.hidden = true;
      sideFormPro.hidden = false;
    });
  }

  // ── ENVOI DIRECT VIA NETLIFY FORMS (sans logiciel mail) ──
  function submitNetlifyForm(formName, fields) {
    var params = new URLSearchParams();
    params.append('form-name', formName);
    Object.keys(fields).forEach(function(key) { params.append(key, fields[key]); });
    return fetch('/', {
      method: 'POST',
      headers: {'Content-Type': 'application/x-www-form-urlencoded'},
      body: params.toString()
    });
  }

  // ── SIDE PANEL RDV ──
  var formPatient = document.getElementById('form-patient');
  if (formPatient) {
    formPatient.addEventListener('submit', function(e) {
      e.preventDefault();
      var nom = document.getElementById('s-nom').value.trim();
      var tel = document.getElementById('s-tel').value.trim();
      var date = document.getElementById('s-date').value;
      var heure = document.getElementById('s-heure').value;
      var soin = document.getElementById('s-soin').value;
      var adresse = document.getElementById('s-adresse').value.trim();
      if (!nom||!tel||!date||!heure||!soin||!adresse) { alert('Merci de remplir tous les champs (*).'); return; }
      if (heure < '09:00' || heure > '16:00') { alert('Les rendez-vous sont possibles entre 09h et 16h.'); return; }
      var btn = document.getElementById('btn-book-patient');
      btn.disabled = true; btn.textContent = 'Envoi en cours…';
      submitNetlifyForm('demande-patient', {
        nom: nom, telephone: tel, date: date, heure: heure, soin: soin, adresse: adresse
      }).then(function(res) {
        btn.disabled = false; btn.textContent = 'Confirmer le rendez-vous';
        if (res.ok) {
          closeSidePanel();
          showModal('📅', 'Demande envoyée !', 'Votre demande a bien été transmise à ' + CONTACT_EMAIL + '. Je vous confirme le rendez-vous par téléphone sous 30 minutes.');
        } else {
          showModal('⚠️', 'Erreur', 'Une erreur est survenue. Merci de m\'appeler directement au 078 447 78 46.');
        }
      }).catch(function() {
        btn.disabled = false; btn.textContent = 'Confirmer le rendez-vous';
        showModal('⚠️', 'Erreur réseau', 'Impossible d\'envoyer la demande. Merci de m\'appeler directement au 078 447 78 46.');
      });
    });
  }

  var formPro = document.getElementById('form-pro');
  if (formPro) {
    formPro.addEventListener('submit', function(e) {
      e.preventDefault();
      var nom = document.getElementById('sp-nom').value.trim();
      var profession = document.getElementById('sp-profession').value;
      var tel = document.getElementById('sp-tel').value.trim();
      var date = document.getElementById('sp-date').value;
      var heure = document.getElementById('sp-heure').value;
      var motif = document.getElementById('sp-motif').value;
      if (!nom||!profession||!tel||!date||!heure||!motif) { alert('Merci de remplir tous les champs (*).'); return; }
      if (heure < '09:00' || heure > '16:00') { alert('Les rendez-vous sont possibles entre 09h et 16h.'); return; }
      var btn = document.getElementById('btn-book-pro');
      btn.disabled = true; btn.textContent = 'Envoi en cours…';
      submitNetlifyForm('demande-pro', {
        nom: nom, profession: profession, telephone: tel, date: date, heure: heure, motif: motif
      }).then(function(res) {
        btn.disabled = false; btn.textContent = 'Envoyer la demande';
        if (res.ok) {
          closeSidePanel();
          showModal('🏥', 'Demande envoyée !', 'Votre demande a bien été transmise à ' + CONTACT_EMAIL + '. Je vous rappelle dans les 30 minutes pour confirmer la prise en charge.');
        } else {
          showModal('⚠️', 'Erreur', 'Une erreur est survenue. Merci de m\'appeler directement au 078 447 78 46.');
        }
      }).catch(function() {
        btn.disabled = false; btn.textContent = 'Envoyer la demande';
        showModal('⚠️', 'Erreur réseau', 'Impossible d\'envoyer la demande. Merci de m\'appeler directement au 078 447 78 46.');
      });
    });
  }

  // Date min
  var today = new Date().toISOString().split('T')[0];
  ['s-date','sp-date'].forEach(function(id) {
    var el = document.getElementById(id); if (el) el.min = today;
  });

  // ── CONTACT FORM (soumission directe via Netlify Forms, sans logiciel mail) ──
  var contactForm = document.getElementById('contact-form');
  if (contactForm) {
    contactForm.addEventListener('submit', function(e) {
      e.preventDefault();
      var form = e.target;
      var prenom = form.querySelector('[name="prenom"]').value.trim();
      var nom = form.querySelector('[name="nom"]').value.trim();
      var contact = form.querySelector('[name="contact"]').value.trim();
      var message = form.querySelector('[name="message"]').value.trim();
      if (!prenom||!nom||!contact||!message) { alert('Merci de remplir tous les champs.'); return; }
      var btn = document.getElementById('btn-contact-send');
      btn.disabled = true; btn.textContent = 'Envoi en cours…';
      var body = new URLSearchParams(new FormData(form)).toString();
      fetch('/', {
        method: 'POST',
        headers: {'Content-Type': 'application/x-www-form-urlencoded'},
        body: body
      }).then(function(res) {
        if (res.ok) {
          showModal('✉️', 'Message envoyé !', 'Merci pour votre message, il a été transmis à ' + CONTACT_EMAIL + '. Je vous répondrai dans les plus brefs délais.');
          form.reset();
        } else {
          showModal('⚠️', 'Erreur', 'Une erreur est survenue. Contactez-moi directement par téléphone ou par email.');
        }
      }).catch(function() {
        showModal('⚠️', 'Erreur réseau', 'Impossible d\'envoyer le message. Contactez-moi par téléphone ou par email.');
      }).finally(function() {
        btn.disabled = false; btn.textContent = 'Envoyer le message';
      });
    });
  }

  // ── MODAL ──
  function showModal(icon, title, text) {
    var modal = document.getElementById('modal');
    if (!modal) return;
    document.getElementById('modal-icon').textContent = icon;
    document.getElementById('modal-title').textContent = title;
    document.getElementById('modal-text').textContent = text;
    modal.classList.add('open');
  }
  function closeModal() {
    var modal = document.getElementById('modal');
    if (modal) modal.classList.remove('open');
  }
  var modalCloseBtn = document.getElementById('modal-close-btn');
  var modalEl = document.getElementById('modal');
  if (modalCloseBtn) modalCloseBtn.addEventListener('click', closeModal);
  if (modalEl) modalEl.addEventListener('click', function(e) { if (e.target === this) closeModal(); });

  // Animations d'entrée de la page (une fois tout défini ci-dessus)
  initReveal();
  initCounters();

});
