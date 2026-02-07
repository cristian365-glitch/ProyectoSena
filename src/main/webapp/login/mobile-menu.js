// mobile-menu.js - VERSION DEBUG
// Maneja el menú móvil desplegable

window.cloneAuthToMobile = null;

(function() {
    'use strict';
    
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
        
        menuToggle.addEventListener('click', function(e) {
            e.stopPropagation();
            const isActive = mobileMenu.classList.contains('active');
            
            if (isActive) {
                closeMobileMenu();
            } else {
                openMobileMenu();
            }
        });
        
        function setupAvatarClick() {
            if (window.innerWidth <= 768) {
                const userMenu = document.querySelector('.user-menu');
                const userAvatar = document.querySelector('.user-avatar');
                
                if (userMenu && userAvatar) {
                    userMenu.onclick = null;
                    userAvatar.onclick = null;
                    
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
        
        setupAvatarClick();
        
        let resizeTimer;
        window.addEventListener('resize', function() {
            clearTimeout(resizeTimer);
            resizeTimer = setTimeout(function() {
                setupAvatarClick();
                
                if (window.innerWidth > 768 && mobileMenu.classList.contains('active')) {
                    closeMobileMenu();
                }
            }, 250);
        });
        
        const mobileMenuLinks = mobileMenu.querySelectorAll('a');
        mobileMenuLinks.forEach(link => {
            link.addEventListener('click', function() {
                closeMobileMenu();
            });
        });
        
        document.addEventListener('click', function(event) {
            if (mobileMenu.classList.contains('active')) {
                if (!mobileMenu.contains(event.target) && !menuToggle.contains(event.target)) {
                    const userMenu = document.querySelector('.user-menu');
                    if (userMenu && !userMenu.contains(event.target)) {
                        closeMobileMenu();
                    }
                }
            }
        });
        
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
        console.log('🔍 DEBUG: cloneAuthToMobile ejecutándose...');
        
        const authContainer = document.getElementById('auth-container');
        const authMobile = document.getElementById('auth-mobile');
        
        if (!authContainer || !authMobile) {
            console.error('❌ No se encontró auth-container o auth-mobile');
            return;
        }
        
        const userMenu = authContainer.querySelector('.user-menu');
        
        if (userMenu) {
            console.log('✅ Usuario logueado detectado');
            
            const userName = authContainer.querySelector('.user-name .user-text')?.textContent || 'Usuario';
            const userAvatar = authContainer.querySelector('.user-avatar')?.src || '';
            const isAdmin = userMenu.classList.contains('admin-menu');
            
            console.log('👤 Datos del usuario:', { userName, isAdmin });
            
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
            
            console.log('🔗 Enlaces generados:', menuLinks);
            
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
            
            console.log('✅ Menú móvil de usuario creado');
            
            // NUEVO: Verificar los href generados
            const generatedLinks = authMobile.querySelectorAll('a[href]');
            console.log('🔍 VERIFICACIÓN DE HREFS:');
            generatedLinks.forEach((link, index) => {
                console.log(`  ${index + 1}. ${link.textContent.trim()} -> ${link.getAttribute('href')}`);
            });
            
        } else {
            console.log('❌ No hay usuario logueado, mostrando botones de login');
            
            authMobile.innerHTML = `
                <div class="auth-mobile-section">
                    <div class="auth-buttons">
                        <a href="/login/Login.html" class="btn-auth login">Iniciar Sesión</a>
                        <a href="/login/Login.html" class="btn-auth register">Registrarse</a>
                    </div>
                </div>
            `;
        }
    }
    
    window.cloneAuthToMobile = cloneAuthToMobile;
})();