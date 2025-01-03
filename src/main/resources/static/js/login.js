function toggleLoginPopup(event) {
    event.preventDefault();
    const popup = document.querySelector('.login-popup');
    popup.classList.toggle('show');

    // 팝업 외부 클릭시 닫기
    document.addEventListener('click', function(e) {
        const popup = document.querySelector('.login-popup');
        const loginBtn = document.querySelector('.login-btn');
        
        if (!popup.contains(e.target) && !loginBtn.contains(e.target)) {
            popup.classList.remove('show');
        }
    });
} 