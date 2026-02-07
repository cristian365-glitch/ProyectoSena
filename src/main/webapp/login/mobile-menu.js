// mobile-menu.js
// Maneja el menú móvil desplegable

// Hacer la función disponible globalmente
window.cloneAuthToMobile = null;

(function() {
    'use strict';
    
    // Esperar a que el DOM esté listo
    document.addEventListener('DOMContentLoaded', function() {
        initMobileMenu();
    });
    
    function initMobileMenu() {
        const menuToggle = document.getElementById('menuToggle');
        const mobileMenu = document.getElementById('mobileMenu');
        
        if (!menuToggle || !mobileMenu) {
            console.warn('No se encontraron elementos del menú móvil');
            return;
        }
        
        // Toggle del menú al hacer click en hamburguesa
        menuToggle.addEventListener('click', function(e) {
            e.stopPropagation();
            const isActive = mobileMenu.classList.contains('active');
            
            if (isActive) {
                closeMobileMenu();
            } else {
                openMobileMenu();
            }
        });
        
        // ====== NUEVO: Click en avatar móvil abre el menú ======
        function setupAvatarClick() {
            // Verificar si estamos en móvil
            if (window.innerWidth <= 768) {
                const userMenu = document.querySelector('.user-menu');
                const userAvatar = document.querySelector('.user-avatar');
                
                if (userMenu && userAvatar) {
                    // Remover listeners previos si existen
                    userMenu.onclick = null;
                    userAvatar.onclick = null;
                    
                    // Click en el contenedor del usuario (incluye avatar y nombre oculto)
                    userMenu.addEventListener('click', function(e) {
                        e.preventDefault();
                        e.stopPropagation();
                        
                        const isActive = mobileMenu.classList.contains('active');
                        if (isActive) {
                            closeMobileMenu();
                        } else {
                            openMobileMenu();
                        }
                    });
                    
                    // También en el avatar directamente por si acaso
                    userAvatar.addEventListener('click', function(e) {
                        e.preventDefault();
                        e.stopPropagation();
                        
                        const isActive = mobileMenu.classList.contains('active');
                        if (isActive) {
                            closeMobileMenu();
                        } else {
                            openMobileMenu();
                        }
                    });
                }
            }
        }
        
        // Configurar click del avatar al cargar
        setupAvatarClick();
        
        // Reconfigurar cuando cambie el tamaño de pantalla
        let resizeTimer;
        window.addEventListener('resize', function() {
            clearTimeout(resizeTimer);
            resizeTimer = setTimeout(function() {
                setupAvatarClick();
                
                // Cerrar menú si cambiamos a desktop
                if (window.innerWidth > 768 && mobileMenu.classList.contains('active')) {
                    closeMobileMenu();
                }
            }, 250);
        });
        // ====== FIN NUEVO ======
        
        // Cerrar menú al hacer click en enlaces
        const mobileMenuLinks = mobileMenu.querySelectorAll('a');
        mobileMenuLinks.forEach(link => {
            link.addEventListener('click', function() {
                closeMobileMenu();
            });
        });
        
        // Cerrar menú al hacer click fuera
        document.addEventListener('click', function(event) {
            if (mobileMenu.classList.contains('active')) {
                if (!mobileMenu.contains(event.target) && !menuToggle.contains(event.target)) {
                    // No cerrar si hicieron click en el avatar/user-menu
                    const userMenu = document.querySelector('.user-menu');
                    if (userMenu && !userMenu.contains(event.target)) {
                        closeMobileMenu();
                    }
                }
            }
        });
        
        // Cerrar con tecla Escape
        document.addEventListener('keydown', function(e) {
            if (e.key === 'Escape' && mobileMenu.classList.contains('active')) {
                closeMobileMenu();
            }
        });
    }
    
    function openMobileMenu() {
        const menuToggle = document.getElementById('menuToggle');
        const mobileMenu = document.getElementById('mobileMenu');
        
        menuToggle.classList.add('active');
        mobileMenu.classList.add('active');
        document.body.classList.add('menu-open');
        
        // Clonar contenido de autenticación al menú móvil
        if (typeof window.cloneAuthToMobile === 'function') {
            window.cloneAuthToMobile();
        }
    }
    
    function closeMobileMenu() {
        const menuToggle = document.getElementById('menuToggle');
        const mobileMenu = document.getElementById('mobileMenu');
        
        menuToggle.classList.remove('active');
        mobileMenu.classList.remove('active');
        document.body.classList.remove('menu-open');
    }
    
    function cloneAuthToMobile() {
        const authContainer = document.getElementById('auth-container');
        const authMobile = document.getElementById('auth-mobile');
        
        if (!authContainer || !authMobile) return;
        
        // Verificar si hay un menú de usuario
        const userMenu = authContainer.querySelector('.user-menu');
        
        if (userMenu) {
            // Usuario logueado - crear versión móvil del menú de usuario
            const userName = authContainer.querySelector('.user-name .user-text')?.textContent || 'Usuario';
            const userAvatar = authContainer.querySelector('.user-avatar')?.src || '';
            const isAdmin = userMenu.classList.contains('admin-menu');
            
            let menuLinks = '';
            if (isAdmin) {
                menuLinks = `
                    <a href="/habitacionesCRUD/gestionar-habitaciones.html" class="menu-item">Gestionar Habitaciones</a>
                    <a href="/usuario/reservas.html" class="menu-item">Ver Reservas</a>
                    <a href="/usuario/perfilUser.html" class="menu-item">Mi Perfil</a>
                    <a href="javascript:auth.cerrarSesion()" class="menu-item logout">Cerrar Sesión</a>
                `;
            } else {
                menuLinks = `
                    <a href="/usuario/reservas.html" class="menu-item">Ver Reservas</a>
                    <a href="/usuario/perfilUser.html" class="menu-item">Mi Perfil</a>
                    <a href="javascript:auth.cerrarSesion()" class="menu-item logout">Cerrar Sesión</a>
                `;
            }
            
            authMobile.innerHTML = `
                <div class="user-menu-mobile">
                    <div class="user-info">
                        <img src="${userAvatar}" alt="${userName}" class="user-avatar">
                        <div class="user-name-display">${userName}</div>
                    </div>
                    <div class="menu-links">
                        ${menuLinks}
                    </div>
                </div>
            `;
        } else {
            // No logueado - mostrar botones de login/registro
            authMobile.innerHTML = `
                <div class="auth-mobile-section">
                    <div class="auth-buttons">
                        <a href="login/Login.html" class="btn-auth login">Iniciar Sesión</a>
                        <a href="login/Login.html" class="btn-auth register">Registrarse</a>
                    </div>
                </div>
            `;
        }
    }
    
    // Exponer la función globalmente
    window.cloneAuthToMobile = cloneAuthToMobile;
})();