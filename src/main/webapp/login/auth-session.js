// auth-session.js
// Gestiona la sesión del usuario (admin o normal) con avatares y notificaciones elegantes

class AuthManager {
    constructor() {
        this.usuario = null;
        this.esAdmin = false;
        this.userId = null;
        this.email = null;
        this.avatarUrl = null;
        this.googlePhotoUrl = null; // Foto de Google
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
                this.googlePhotoUrl = data.googlePhotoUrl || data.fotoUrl || null;
                
                // Cargar avatar del usuario
                await this.cargarAvatar();
                
                this.actualizarUI();
                
                console.log('✅ Sesión activa:', {
                    usuario: this.usuario,
                    esAdmin: this.esAdmin,
                    avatar: this.avatarUrl
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
     * Carga la URL del avatar de Google (sin Gravatar)
     */
    async cargarAvatar() {
        // Si hay foto de Google, usarla
        if (this.googlePhotoUrl) {
            this.avatarUrl = this.googlePhotoUrl;
            console.log('✅ Usando foto de Google');
        } else {
            // No hay foto, usar icono por defecto
            this.avatarUrl = null;
            console.log('ℹ️ Sin foto de perfil, usando icono por defecto');
        }
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
    
    // Generar HTML del avatar
    const avatarHTML = this.avatarUrl 
        ? `<img src="${this.avatarUrl}" alt="${nombreCorto}" class="user-avatar" onerror="this.src='/recursos/icons/usuario.svg'">`
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
                    <a href="#" onclick="window.location.href='/habitacionesCRUD/gestionar-habitaciones.html'; return false;" class="menu-item">Gestionar Habitaciones</a>
                    <a href="#" onclick="window.location.href='/usuario/reservas.html'; return false;" class="menu-item">Ver Reservas</a>
                    <a href="#" onclick="window.location.href='/usuario/perfilUser.html'; return false;" class="menu-item">Mi Perfil</a>
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
                    <a href="#" onclick="window.location.href='/usuario/reservas.html'; return false;" class="menu-item">Ver Reservas</a>
                    <a href="#" onclick="window.location.href='/usuario/perfilUser.html'; return false;" class="menu-item">Mi Perfil</a>
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
    
    if (urlParams.get('login') === 'exitoso') {
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