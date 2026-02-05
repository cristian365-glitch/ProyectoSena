// auth-session.js
// Gestiona la sesión del usuario (admin o normal) con avatares de Google/Gravatar y notificaciones elegantes

class AuthManager {
    constructor() {
        this.usuario = null;
        this.esAdmin = false;
        this.userId = null;
        this.email = null;
        this.avatarUrl = null;
        this.googlePhotoUrl = null; // NUEVO: Foto de Google
        this.init();
    }
    
    async init() {
        try {
            // Hacer petición al servlet de verificación de sesión
            const response = await fetch('/VerificaSesionServlet?action=verificarSesion');
 
            const contentType = response.headers.get("content-type");
            if (!contentType || !contentType.includes("application/json")) {
                console.warn('Respuesta no es JSON, usuario no logueado');
                this.mostrarBotonesLogin();
                return;
            }
            
            const data = await response.json();
            
            if (data.logueado) {
                this.usuario = data.nombre;
                this.esAdmin = data.esAdmin === true;
                this.userId = data.userId;
                this.email = data.email;
                
                // NUEVO: Obtener foto de Google si existe
                this.googlePhotoUrl = data.googlePhotoUrl || null;
                
                // Cargar avatar (Google tiene prioridad sobre Gravatar)
                await this.cargarAvatar();
                
                this.actualizarUI();
                
                console.log('✅ Sesión activa:', {
                    usuario: this.usuario,
                    esAdmin: this.esAdmin,
                    avatar: this.avatarUrl,
                    googlePhoto: this.googlePhotoUrl
                });
            } else {
                this.mostrarBotonesLogin();
            }
        } catch (error) {
            console.error('Error al verificar sesión:', error);
            this.mostrarBotonesLogin();
        }
    }
    
    /**
     * Carga la URL del avatar con PRIORIDAD: Google > Gravatar
     */
    async cargarAvatar() {
        try {
            // PRIORIDAD 1: Si hay foto de Google, usarla directamente
            if (this.googlePhotoUrl) {
                this.avatarUrl = this.googlePhotoUrl;
                console.log('✅ Usando foto de Google:', this.avatarUrl);
                return;
            }
            
            // PRIORIDAD 2: Cargar desde Gravatar
            const response = await fetch('/LoginServlet?action=getAvatarUrl&size=200');
            
            if (response.ok) {
                const data = await response.json();
                this.avatarUrl = data.avatarUrl;
                console.log('✅ Usando avatar de Gravatar');
            } else {
                // Fallback: generar URL localmente si el servlet falla
                this.avatarUrl = this.generarAvatarLocal(this.email);
            }
        } catch (error) {
            console.error('Error al cargar avatar:', error);
            // Fallback: generar URL localmente
            this.avatarUrl = this.generarAvatarLocal(this.email);
        }
    }
    
    /**
     * Genera la URL del avatar directamente usando MD5 (fallback)
     */
    generarAvatarLocal(email) {
        if (!email) {
            return 'https://www.gravatar.com/avatar/00000000000000000000000000000000?d=mp&s=200';
        }
        
        const emailHash = this.md5(email.trim().toLowerCase());
        return `https://www.gravatar.com/avatar/${emailHash}?d=mp&s=200`;
    }
    
    /**
     * Función MD5 para generar hash del email
     */
    md5(string) {
        function rotateLeft(lValue, iShiftBits) {
            return (lValue << iShiftBits) | (lValue >>> (32 - iShiftBits));
        }
        
        function addUnsigned(lX, lY) {
            const lX8 = (lX & 0x80000000);
            const lY8 = (lY & 0x80000000);
            const lX4 = (lX & 0x40000000);
            const lY4 = (lY & 0x40000000);
            const lResult = (lX & 0x3FFFFFFF) + (lY & 0x3FFFFFFF);
            if (lX4 & lY4) return (lResult ^ 0x80000000 ^ lX8 ^ lY8);
            if (lX4 | lY4) {
                if (lResult & 0x40000000) return (lResult ^ 0xC0000000 ^ lX8 ^ lY8);
                else return (lResult ^ 0x40000000 ^ lX8 ^ lY8);
            } else {
                return (lResult ^ lX8 ^ lY8);
            }
        }
        
        function F(x, y, z) { return (x & y) | ((~x) & z); }
        function G(x, y, z) { return (x & z) | (y & (~z)); }
        function H(x, y, z) { return (x ^ y ^ z); }
        function I(x, y, z) { return (y ^ (x | (~z))); }
        
        function FF(a, b, c, d, x, s, ac) {
            a = addUnsigned(a, addUnsigned(addUnsigned(F(b, c, d), x), ac));
            return addUnsigned(rotateLeft(a, s), b);
        }
        
        function GG(a, b, c, d, x, s, ac) {
            a = addUnsigned(a, addUnsigned(addUnsigned(G(b, c, d), x), ac));
            return addUnsigned(rotateLeft(a, s), b);
        }
        
        function HH(a, b, c, d, x, s, ac) {
            a = addUnsigned(a, addUnsigned(addUnsigned(H(b, c, d), x), ac));
            return addUnsigned(rotateLeft(a, s), b);
        }
        
        function II(a, b, c, d, x, s, ac) {
            a = addUnsigned(a, addUnsigned(addUnsigned(I(b, c, d), x), ac));
            return addUnsigned(rotateLeft(a, s), b);
        }
        
        function convertToWordArray(string) {
            let lWordCount;
            const lMessageLength = string.length;
            const lNumberOfWords_temp1 = lMessageLength + 8;
            const lNumberOfWords_temp2 = (lNumberOfWords_temp1 - (lNumberOfWords_temp1 % 64)) / 64;
            const lNumberOfWords = (lNumberOfWords_temp2 + 1) * 16;
            const lWordArray = new Array(lNumberOfWords - 1);
            let lBytePosition = 0;
            let lByteCount = 0;
            while (lByteCount < lMessageLength) {
                lWordCount = (lByteCount - (lByteCount % 4)) / 4;
                lBytePosition = (lByteCount % 4) * 8;
                lWordArray[lWordCount] = (lWordArray[lWordCount] | (string.charCodeAt(lByteCount) << lBytePosition));
                lByteCount++;
            }
            lWordCount = (lByteCount - (lByteCount % 4)) / 4;
            lBytePosition = (lByteCount % 4) * 8;
            lWordArray[lWordCount] = lWordArray[lWordCount] | (0x80 << lBytePosition);
            lWordArray[lNumberOfWords - 2] = lMessageLength << 3;
            lWordArray[lNumberOfWords - 1] = lMessageLength >>> 29;
            return lWordArray;
        }
        
        function wordToHex(lValue) {
            let wordToHexValue = "", wordToHexValue_temp = "", lByte, lCount;
            for (lCount = 0; lCount <= 3; lCount++) {
                lByte = (lValue >>> (lCount * 8)) & 255;
                wordToHexValue_temp = "0" + lByte.toString(16);
                wordToHexValue = wordToHexValue + wordToHexValue_temp.substr(wordToHexValue_temp.length - 2, 2);
            }
            return wordToHexValue;
        }
        
        const x = convertToWordArray(string);
        let a = 0x67452301, b = 0xEFCDAB89, c = 0x98BADCFE, d = 0x10325476;
        
        for (let k = 0; k < x.length; k += 16) {
            const AA = a, BB = b, CC = c, DD = d;
            a = FF(a, b, c, d, x[k + 0], 7, 0xD76AA478);
            d = FF(d, a, b, c, x[k + 1], 12, 0xE8C7B756);
            c = FF(c, d, a, b, x[k + 2], 17, 0x242070DB);
            b = FF(b, c, d, a, x[k + 3], 22, 0xC1BDCEEE);
            a = FF(a, b, c, d, x[k + 4], 7, 0xF57C0FAF);
            d = FF(d, a, b, c, x[k + 5], 12, 0x4787C62A);
            c = FF(c, d, a, b, x[k + 6], 17, 0xA8304613);
            b = FF(b, c, d, a, x[k + 7], 22, 0xFD469501);
            a = FF(a, b, c, d, x[k + 8], 7, 0x698098D8);
            d = FF(d, a, b, c, x[k + 9], 12, 0x8B44F7AF);
            c = FF(c, d, a, b, x[k + 10], 17, 0xFFFF5BB1);
            b = FF(b, c, d, a, x[k + 11], 22, 0x895CD7BE);
            a = FF(a, b, c, d, x[k + 12], 7, 0x6B901122);
            d = FF(d, a, b, c, x[k + 13], 12, 0xFD987193);
            c = FF(c, d, a, b, x[k + 14], 17, 0xA679438E);
            b = FF(b, c, d, a, x[k + 15], 22, 0x49B40821);
            a = GG(a, b, c, d, x[k + 1], 5, 0xF61E2562);
            d = GG(d, a, b, c, x[k + 6], 9, 0xC040B340);
            c = GG(c, d, a, b, x[k + 11], 14, 0x265E5A51);
            b = GG(b, c, d, a, x[k + 0], 20, 0xE9B6C7AA);
            a = GG(a, b, c, d, x[k + 5], 5, 0xD62F105D);
            d = GG(d, a, b, c, x[k + 10], 9, 0x2441453);
            c = GG(c, d, a, b, x[k + 15], 14, 0xD8A1E681);
            b = GG(b, c, d, a, x[k + 4], 20, 0xE7D3FBC8);
            a = GG(a, b, c, d, x[k + 9], 5, 0x21E1CDE6);
            d = GG(d, a, b, c, x[k + 14], 9, 0xC33707D6);
            c = GG(c, d, a, b, x[k + 3], 14, 0xF4D50D87);
            b = GG(b, c, d, a, x[k + 8], 20, 0x455A14ED);
            a = GG(a, b, c, d, x[k + 13], 5, 0xA9E3E905);
            d = GG(d, a, b, c, x[k + 2], 9, 0xFCEFA3F8);
            c = GG(c, d, a, b, x[k + 7], 14, 0x676F02D9);
            b = GG(b, c, d, a, x[k + 12], 20, 0x8D2A4C8A);
            a = HH(a, b, c, d, x[k + 5], 4, 0xFFFA3942);
            d = HH(d, a, b, c, x[k + 8], 11, 0x8771F681);
            c = HH(c, d, a, b, x[k + 11], 16, 0x6D9D6122);
            b = HH(b, c, d, a, x[k + 14], 23, 0xFDE5380C);
            a = HH(a, b, c, d, x[k + 1], 4, 0xA4BEEA44);
            d = HH(d, a, b, c, x[k + 4], 11, 0x4BDECFA9);
            c = HH(c, d, a, b, x[k + 7], 16, 0xF6BB4B60);
            b = HH(b, c, d, a, x[k + 10], 23, 0xBEBFBC70);
            a = HH(a, b, c, d, x[k + 13], 4, 0x289B7EC6);
            d = HH(d, a, b, c, x[k + 0], 11, 0xEAA127FA);
            c = HH(c, d, a, b, x[k + 3], 16, 0xD4EF3085);
            b = HH(b, c, d, a, x[k + 6], 23, 0x4881D05);
            a = HH(a, b, c, d, x[k + 9], 4, 0xD9D4D039);
            d = HH(d, a, b, c, x[k + 12], 11, 0xE6DB99E5);
            c = HH(c, d, a, b, x[k + 15], 16, 0x1FA27CF8);
            b = HH(b, c, d, a, x[k + 2], 23, 0xC4AC5665);
            a = II(a, b, c, d, x[k + 0], 6, 0xF4292244);
            d = II(d, a, b, c, x[k + 7], 10, 0x432AFF97);
            c = II(c, d, a, b, x[k + 14], 15, 0xAB9423A7);
            b = II(b, c, d, a, x[k + 5], 21, 0xFC93A039);
            a = II(a, b, c, d, x[k + 12], 6, 0x655B59C3);
            d = II(d, a, b, c, x[k + 3], 10, 0x8F0CCC92);
            c = II(c, d, a, b, x[k + 10], 15, 0xFFEFF47D);
            b = II(b, c, d, a, x[k + 1], 21, 0x85845DD1);
            a = II(a, b, c, d, x[k + 8], 6, 0x6FA87E4F);
            d = II(d, a, b, c, x[k + 15], 10, 0xFE2CE6E0);
            c = II(c, d, a, b, x[k + 6], 15, 0xA3014314);
            b = II(b, c, d, a, x[k + 13], 21, 0x4E0811A1);
            a = II(a, b, c, d, x[k + 4], 6, 0xF7537E82);
            d = II(d, a, b, c, x[k + 11], 10, 0xBD3AF235);
            c = II(c, d, a, b, x[k + 2], 15, 0x2AD7D2BB);
            b = II(b, c, d, a, x[k + 9], 21, 0xEB86D391);
            a = addUnsigned(a, AA);
            b = addUnsigned(b, BB);
            c = addUnsigned(c, CC);
            d = addUnsigned(d, DD);
        }
        
        return (wordToHex(a) + wordToHex(b) + wordToHex(c) + wordToHex(d)).toLowerCase();
    }
    
    /**
     * Limita el nombre a solo dos palabras (nombre y apellido)
     */
    limitarNombre(nombreCompleto) {
        if (!nombreCompleto) return nombreCompleto;
        const palabras = nombreCompleto.trim().split(' ');
        return palabras.slice(0, 2).join(' ');
    }

    actualizarUI() {
        const authContainer = document.getElementById('auth-container');
        
        if (!authContainer) {
            console.warn('No se encontró #auth-container en el HTML');
            return;
        }
        
        authContainer.innerHTML = '';
        
        // Limitar nombre a dos palabras
        const nombreCorto = this.limitarNombre(this.usuario);
        
        // Generar HTML del avatar con foto de Google o Gravatar
        const avatarHTML = this.avatarUrl 
            ? `<img src="${this.avatarUrl}" alt="${nombreCorto}" class="user-avatar" onerror="this.src='https://www.gravatar.com/avatar/00000000000000000000000000000000?d=mp&s=200'">`
            : `<img src="/recursos/icons/usuario.svg" alt="${nombreCorto}" class="icon" width="23px">`;
        
        if (this.esAdmin) {
            // Admin logueado
            authContainer.innerHTML = `
                <div class="user-menu admin-menu">
                    <span class="user-name">
                        ${avatarHTML}
                        <span class="user-text">⭐ ${nombreCorto}</span>
                    </span>
                    <div class="dropdown-menu">
                        <a href="/habitacionesCRUD/gestionar-habitaciones.html" class="menu-item">Gestionar Habitaciones</a>
                        <a href="/usuario/reservas.html" class="menu-item">Ver Reservas</a>
                        <a href="/usuario/perfilUser.html" class="menu-item">Mi Perfil</a>
                        <hr>
                        <a href="javascript:auth.cerrarSesion()" class="menu-item logout">Cerrar Sesión</a>
                    </div>
                </div>
            `;
        } else {
            // Usuario normal logueado
            authContainer.innerHTML = `
                <div class="user-menu client-menu">
                    <span class="user-name">
                        ${avatarHTML}
                        <span class="user-text">${nombreCorto}</span>
                    </span>
                    <div class="dropdown-menu">
                        <a href="/usuario/reservas.html" class="menu-item">Ver Reservas</a>
                        <a href="/usuario/perfilUser.html" class="menu-item">Mi Perfil</a>
                        <hr>
                        <a href="javascript:auth.cerrarSesion()" class="menu-item logout">Cerrar Sesión</a>
                    </div>
                </div>
            `;
        }
        
        this.agregarEventosDropdown();
    }
    
    mostrarBotonesLogin() {
        const authContainer = document.getElementById('auth-container');
        
        if (!authContainer) {
            console.warn('No se encontró #auth-container en el HTML');
            return;
        }
        
        authContainer.innerHTML = `
            <a href="/login/Login.html" id="login-link" class="btn-auth login">
                <img src="/recursos/icons/usuario.svg" class="icon" width="23px" alt="icon usuario" onerror="this.style.display='none'">
                INICIAR SESIÓN
            </a>
        `;
    }
    
    agregarEventosDropdown() {
        const userMenu = document.querySelector('.user-menu');
        const userName = document.querySelector('.user-name');
        
        if (userName) {
            userName.addEventListener('click', function(e) {
                e.stopPropagation();
                
                // Verificar si estamos en móvil (768px o menos)
                if (window.innerWidth <= 768) {
                    // En móvil: abrir el menú hamburguesa
                    const menuToggle = document.getElementById('menuToggle');
                    const mobileMenu = document.getElementById('mobileMenu');
                    
                    if (menuToggle && mobileMenu) {
                        menuToggle.classList.toggle('active');
                        mobileMenu.classList.toggle('active');
                        document.body.classList.toggle('menu-open');
                        
                        // Clonar contenido de autenticación al menú móvil
                        const authMobile = document.getElementById('auth-mobile');
                        if (authMobile && typeof cloneAuthToMobile === 'function') {
                            cloneAuthToMobile();
                        }
                    }
                } else {
                    // En desktop: toggle del dropdown normal
                    userMenu.classList.toggle('active');
                }
            });
        }
        
        // Cerrar dropdown en desktop cuando se hace click fuera
        document.addEventListener('click', function(event) {
            if (userMenu && !userMenu.contains(event.target) && window.innerWidth > 768) {
                userMenu.classList.remove('active');
            }
        });
    }
    
    cerrarSesion() {
        notify.confirm(
            '¿Cerrar sesión?',
            '¿Estás seguro de que deseas cerrar tu sesión?',
            () => {
                window.location.href = '/LoginServlet?action=cerrarSesion';
            }
        );
    }
    
    estaLogueado() {
        return this.usuario !== null;
    }
    
    esAdministrador() {
        return this.esAdmin === true;
    }
}

// ============================================
// SISTEMA DE NOTIFICACIONES ELEGANTES
// ============================================

class NotificationManager {
    constructor() {
        this.injectStyles();
    }
    
    injectStyles() {
        if (document.getElementById('notification-styles')) return;
        
        const style = document.createElement('style');
        style.id = 'notification-styles';
        style.textContent = `
            /* NOTIFICACIONES TOAST */
            .armonia-notification {
                position: fixed;
                top: 100px;
                right: 30px;
                min-width: 320px;
                max-width: 420px;
                background: white;
                border-radius: 12px;
                box-shadow: 0 10px 40px rgba(0, 0, 0, 0.15);
                z-index: 10000;
                overflow: hidden;
                animation: slideInRight 0.5s cubic-bezier(0.68, -0.55, 0.265, 1.55);
                font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
            }
            
            .armonia-notification.hiding {
                animation: slideOutRight 0.3s ease-in forwards;
            }
            
            /* MODAL DE CONFIRMACIÓN */
            .armonia-modal-overlay {
                position: fixed;
                top: 0;
                left: 0;
                right: 0;
                bottom: 0;
                background: rgba(0, 0, 0, 0.5);
                backdrop-filter: blur(4px);
                z-index: 9999;
                display: flex;
                align-items: center;
                justify-content: center;
                animation: fadeIn 0.2s ease;
            }
            
            .armonia-modal {
                background: white;
                border-radius: 16px;
                box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
                max-width: 420px;
                width: calc(100% - 40px);
                overflow: hidden;
                animation: modalSlideIn 0.3s cubic-bezier(0.68, -0.55, 0.265, 1.55);
            }
            
            @keyframes fadeIn {
                from { opacity: 0; }
                to { opacity: 1; }
            }
            
            @keyframes modalSlideIn {
                from {
                    transform: scale(0.8) translateY(-20px);
                    opacity: 0;
                }
                to {
                    transform: scale(1) translateY(0);
                    opacity: 1;
                }
            }
            
            .armonia-modal.closing {
                animation: modalSlideOut 0.2s ease forwards;
            }
            
            @keyframes modalSlideOut {
                from {
                    transform: scale(1) translateY(0);
                    opacity: 1;
                }
                to {
                    transform: scale(0.8) translateY(-20px);
                    opacity: 0;
                }
            }
            
            .modal-header {
                padding: 24px 24px 16px;
                border-bottom: 1px solid rgba(0, 0, 0, 0.06);
            }
            
            .modal-icon {
                width: 48px;
                height: 48px;
                border-radius: 50%;
                background: linear-gradient(135deg, #ed8936, #dd6b20);
                color: white;
                display: flex;
                align-items: center;
                justify-content: center;
                font-size: 24px;
                margin: 0 auto 16px;
            }
            
            .modal-title {
                font-size: 20px;
                font-weight: 600;
                color: #1a202c;
                text-align: center;
                margin: 0;
            }
            
            .modal-body {
                padding: 20px 24px 24px;
            }
            
            .modal-message {
                color: #4a5568;
                font-size: 15px;
                line-height: 1.6;
                text-align: center;
            }
            
            .modal-actions {
                display: flex;
                gap: 12px;
                padding: 0 24px 24px;
            }
            
            .modal-btn {
                flex: 1;
                padding: 12px 24px;
                border-radius: 8px;
                border: none;
                font-size: 14px;
                font-weight: 600;
                cursor: pointer;
                transition: all 0.2s;
                font-family: inherit;
            }
            
            .modal-btn-cancel {
                background: #edf2f7;
                color: #4a5568;
            }
            
            .modal-btn-cancel:hover {
                background: #e2e8f0;
            }
            
            .modal-btn-confirm {
                background: linear-gradient(135deg, #d4af37, #b8860b);
                color: white;
            }
            
            .modal-btn-confirm:hover {
                transform: translateY(-1px);
                box-shadow: 0 4px 12px rgba(212, 175, 55, 0.4);
            }
            
            @keyframes slideInRight {
                from {
                    transform: translateX(450px);
                    opacity: 0;
                }
                to {
                    transform: translateX(0);
                    opacity: 1;
                }
            }
            
            @keyframes slideOutRight {
                from {
                    transform: translateX(0);
                    opacity: 1;
                }
                to {
                    transform: translateX(450px);
                    opacity: 0;
                }
            }
            
            .notification-header {
                padding: 20px 24px 16px;
                border-bottom: 1px solid rgba(0, 0, 0, 0.06);
                display: flex;
                align-items: center;
                gap: 12px;
            }
            
            .notification-icon {
                width: 40px;
                height: 40px;
                border-radius: 50%;
                display: flex;
                align-items: center;
                justify-content: center;
                font-size: 20px;
                flex-shrink: 0;
            }
            
            .notification-icon.success {
                background: linear-gradient(135deg, #d4af37, #b8860b);
                color: white;
            }
            
            .notification-icon.info {
                background: linear-gradient(135deg, #4299e1, #3182ce);
                color: white;
            }
            
            .notification-icon.warning {
                background: linear-gradient(135deg, #ed8936, #dd6b20);
                color: white;
            }
            
            .notification-icon.error {
                background: linear-gradient(135deg, #f56565, #c53030);
                color: white;
            }
            
            .notification-title {
                flex: 1;
                font-size: 16px;
                font-weight: 600;
                color: #1a202c;
                letter-spacing: 0.3px;
            }
            
            .notification-close {
                width: 28px;
                height: 28px;
                border-radius: 50%;
                background: transparent;
                border: none;
                cursor: pointer;
                display: flex;
                align-items: center;
                justify-content: center;
                color: #a0aec0;
                transition: all 0.2s;
                font-size: 18px;
                line-height: 1;
            }
            
            .notification-close:hover {
                background: #edf2f7;
                color: #4a5568;
            }
            
            .notification-body {
                padding: 16px 24px 20px;
            }
            
            .notification-message {
                color: #4a5568;
                font-size: 14px;
                line-height: 1.6;
            }
            
            .notification-progress {
                position: absolute;
                bottom: 0;
                left: 0;
                height: 3px;
                background: linear-gradient(90deg, #d4af37, #b8860b);
                animation: progressBar 4s linear forwards;
            }
            
            @keyframes progressBar {
                from { width: 100%; }
                to { width: 0%; }
            }
            
            /* Responsive */
            @media (max-width: 768px) {
                .armonia-notification {
                    right: 15px;
                    left: 15px;
                    min-width: auto;
                    max-width: none;
                    top: 80px;
                }
                
                .armonia-modal {
                    max-width: calc(100% - 40px);
                }
                
                @keyframes slideInRight {
                    from {
                        transform: translateY(-100px);
                        opacity: 0;
                    }
                    to {
                        transform: translateY(0);
                        opacity: 1;
                    }
                }
                
                @keyframes slideOutRight {
                    from {
                        transform: translateY(0);
                        opacity: 1;
                    }
                    to {
                        transform: translateY(-100px);
                        opacity: 0;
                    }
                }
            }
        `;
        document.head.appendChild(style);
    }
    
    show(type, title, message, duration = 4000) {
        const existing = document.querySelectorAll('.armonia-notification');
        existing.forEach(notif => notif.remove());
        
        const notification = document.createElement('div');
        notification.className = 'armonia-notification';
        
        const icons = {
            success: '✓',
            info: 'ℹ',
            warning: '⚠',
            error: '✕'
        };
        
        notification.innerHTML = `
            <div class="notification-header">
                <div class="notification-icon ${type}">
                    ${icons[type] || icons.info}
                </div>
                <div class="notification-title">${title}</div>
                <button class="notification-close" aria-label="Cerrar">×</button>
            </div>
            <div class="notification-body">
                <div class="notification-message">${message}</div>
            </div>
            <div class="notification-progress"></div>
        `;
        
        document.body.appendChild(notification);
        
        const closeBtn = notification.querySelector('.notification-close');
        closeBtn.addEventListener('click', () => this.hide(notification));
        
        if (duration > 0) {
            setTimeout(() => this.hide(notification), duration);
        }
        
        return notification;
    }
    
    hide(notification) {
        notification.classList.add('hiding');
        setTimeout(() => notification.remove(), 300);
    }
    
    confirm(title, message, onConfirm, onCancel) {
        const overlay = document.createElement('div');
        overlay.className = 'armonia-modal-overlay';
        
        const modal = document.createElement('div');
        modal.className = 'armonia-modal';
        
        modal.innerHTML = `
            <div class="modal-header">
                <div class="modal-icon">⚠</div>
                <h3 class="modal-title">${title}</h3>
            </div>
            <div class="modal-body">
                <p class="modal-message">${message}</p>
            </div>
            <div class="modal-actions">
                <button class="modal-btn modal-btn-cancel">Cancelar</button>
                <button class="modal-btn modal-btn-confirm">Aceptar</button>
            </div>
        `;
        
        overlay.appendChild(modal);
        document.body.appendChild(overlay);
        
        const closeModal = () => {
            modal.classList.add('closing');
            setTimeout(() => overlay.remove(), 200);
        };
        
        const cancelBtn = modal.querySelector('.modal-btn-cancel');
        cancelBtn.addEventListener('click', () => {
            closeModal();
            if (onCancel) onCancel();
        });
        
        const confirmBtn = modal.querySelector('.modal-btn-confirm');
        confirmBtn.addEventListener('click', () => {
            closeModal();
            if (onConfirm) onConfirm();
        });
        
        overlay.addEventListener('click', (e) => {
            if (e.target === overlay) {
                closeModal();
                if (onCancel) onCancel();
            }
        });
        
        const handleEscape = (e) => {
            if (e.key === 'Escape') {
                closeModal();
                if (onCancel) onCancel();
                document.removeEventListener('keydown', handleEscape);
            }
        };
        document.addEventListener('keydown', handleEscape);
        
        return overlay;
    }
    
    success(title, message, duration) {
        return this.show('success', title, message, duration);
    }
    
    info(title, message, duration) {
        return this.show('info', title, message, duration);
    }
    
    warning(title, message, duration) {
        return this.show('warning', title, message, duration);
    }
    
    error(title, message, duration) {
        return this.show('error', title, message, duration);
    }
}

// ============================================
// INICIALIZACIÓN GLOBAL
// ============================================

let auth;
let notify;

document.addEventListener('DOMContentLoaded', function() {
    auth = new AuthManager();
    notify = new NotificationManager();
    
    const urlParams = new URLSearchParams(window.location.search);
    
    if (urlParams.get('registro') === 'exitoso') {
        setTimeout(() => {
            notify.success(
                '¡Bienvenido a Hotel Armonía!',
                'Tu cuenta ha sido creada exitosamente. Explora nuestras experiencias únicas.',
                5000
            );
        }, 500);
        window.history.replaceState({}, document.title, window.location.pathname);
    }
    
    if (urlParams.get('login') === 'exitoso' || urlParams.get('login') === 'google') {
        setTimeout(() => {
            const nombre = auth.usuario || 'Usuario';
            notify.success(
                '¡Bienvenido de vuelta!',
                `Hola ${nombre}, es un placer verte de nuevo.`,
                4000
            );
        }, 500);
        window.history.replaceState({}, document.title, window.location.pathname);
    }
    
    if (urlParams.get('login') === 'admin') {
        setTimeout(() => {
            notify.info(
                'Panel de Administración',
                'Has iniciado sesión como administrador.',
                4000
            );
        }, 500);
        window.history.replaceState({}, document.title, window.location.pathname);
    }
    
    if (urlParams.get('logout') === 'exitoso') {
        notify.info(
            'Sesión Cerrada',
            'Has cerrado sesión correctamente. ¡Esperamos verte pronto!',
            4000
        );
        window.history.replaceState({}, document.title, window.location.pathname);
    }
});