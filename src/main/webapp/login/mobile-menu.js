// mobile-menu.js
// Maneja el menú hamburguesa y la integración móvil con autenticación

class MobileMenuManager {
    constructor() {
        this.isOpen = false;
        this.menuToggle = null;
        this.mobileMenu = null;
        this.init();
    }

    init() {
        // Esperar a que el DOM esté listo
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', () => this.setup());
        } else {
            this.setup();
        }
    }

    setup() {
        // Crear estructura del menú hamburguesa
        this.createMobileMenuStructure();
        
        // Obtener referencias
        this.menuToggle = document.querySelector('.menu-toggle');
        this.mobileMenu = document.querySelector('.nav-mobile-menu');
        
        if (!this.menuToggle || !this.mobileMenu) {
            console.warn('Mobile menu elements not found');
            return;
        }

        // Event listeners
        this.menuToggle.addEventListener('click', (e) => {
            e.stopPropagation();
            this.toggle();
        });
        
        // Cerrar menú al hacer clic en un enlace
        const menuLinks = this.mobileMenu.querySelectorAll('a');
        menuLinks.forEach(link => {
            link.addEventListener('click', () => this.close());
        });

        // Cerrar menú al hacer clic fuera
        document.addEventListener('click', (e) => {
            if (this.isOpen && 
                !this.mobileMenu.contains(e.target) && 
                !this.menuToggle.contains(e.target)) {
                this.close();
            }
        });

        // Cerrar menú con tecla Escape
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape' && this.isOpen) {
                this.close();
            }
        });

        // Actualizar menú cuando cambie el estado de autenticación
        this.setupAuthObserver();
    }

    createMobileMenuStructure() {
        // Verificar si ya existe
        if (document.querySelector('.menu-toggle-li')) {
            return;
        }

        const header = document.querySelector('header ul');
        if (!header) return;

        // Agregar botón hamburguesa DENTRO del li.registro
        const registroLi = header.querySelector('.registro');
        if (registroLi) {
            // Crear el elemento del menú toggle
            const menuToggleBtn = document.createElement('button');
            menuToggleBtn.className = 'menu-toggle';
            menuToggleBtn.setAttribute('aria-label', 'Menú');
            menuToggleBtn.innerHTML = `
                <span></span>
                <span></span>
                <span></span>
            `;
            
            // Crear li para el toggle
            const menuToggleLi = document.createElement('li');
            menuToggleLi.className = 'menu-toggle-li';
            menuToggleLi.appendChild(menuToggleBtn);
            
            // Insertar DESPUÉS del .registro para que esté a la derecha
            registroLi.parentNode.insertBefore(menuToggleLi, registroLi.nextSibling);
        }

        // Crear menú móvil desplegable
        const mobileMenuDiv = document.createElement('div');
        mobileMenuDiv.className = 'nav-mobile-menu';
        
        // Obtener enlaces del menú principal (excepto logo y registro)
        const mainMenuLinks = Array.from(header.querySelectorAll('li:not(.logo):not(.registro):not(.menu-toggle-li) a'));
        
        let mobileMenuHTML = '<ul>';
        mainMenuLinks.forEach(link => {
            const href = link.getAttribute('href') || '#';
            const text = link.textContent.trim();
            if (text) { // Solo agregar si hay texto
                mobileMenuHTML += `<li><a href="${href}">${text}</a></li>`;
            }
        });
        mobileMenuHTML += '</ul>';

        // Agregar sección de autenticación (se actualizará dinámicamente)
        mobileMenuHTML += '<div class="auth-mobile-container"></div>';

        mobileMenuDiv.innerHTML = mobileMenuHTML;

        // Insertar después del header
        const headerElement = document.querySelector('header');
        if (headerElement && headerElement.parentNode) {
            headerElement.parentNode.insertBefore(mobileMenuDiv, headerElement.nextSibling);
        }
    }

    updateAuthSection() {
        const authContainer = document.querySelector('.auth-mobile-container');
        if (!authContainer) return;

        // Verificar si el usuario está autenticado
        if (window.auth && window.auth.usuario) {
            // Usuario autenticado
            const userName = window.auth.usuario;
            const avatarUrl = window.auth.avatarUrl || 'https://www.gravatar.com/avatar/00000000000000000000000000000000?d=mp&s=200';
            const isAdmin = window.auth.esAdmin;

            authContainer.innerHTML = `
                <div class="user-menu-mobile">
                    <div class="user-info">
                        <img src="${avatarUrl}" alt="${userName}" class="user-avatar">
                        <div class="user-name-display">${userName}</div>
                    </div>
                    <div class="menu-links">
                        ${isAdmin ? 
                            '<a href="../admin/dashboard.html" class="menu-item">📊 Dashboard</a>' :
                            '<a href="../login/mis-reservas.html" class="menu-item">📅 Mis Reservas</a>'
                        }
                        <a href="../login/perfil.html" class="menu-item">👤 Mi Perfil</a>
                        ${isAdmin ? '<a href="../admin/gestion-habitaciones.html" class="menu-item">🏠 Habitaciones</a>' : ''}
                        ${isAdmin ? '<a href="../admin/gestion-reservas.html" class="menu-item">📋 Reservas</a>' : ''}
                        ${isAdmin ? '<a href="../admin/gestion-usuarios.html" class="menu-item">👥 Usuarios</a>' : ''}
                        <a href="#" onclick="cerrarSesion(); return false;" class="menu-item logout">🚪 Cerrar Sesión</a>
                    </div>
                </div>
            `;
        } else {
            // Usuario no autenticado
            authContainer.innerHTML = `
                <div class="auth-mobile-section">
                    <div class="auth-buttons">
                        <a href="../login/Login.html" class="btn-auth login">Iniciar Sesión</a>
                        <a href="../login/Registro.html" class="btn-auth register">Registrarse</a>
                    </div>
                </div>
            `;
        }
    }

    setupAuthObserver() {
        // Actualizar cuando auth esté disponible
        const checkAuth = setInterval(() => {
            if (window.auth !== undefined) {
                this.updateAuthSection();
                clearInterval(checkAuth);
                
                // Observar cambios futuros
                const originalInit = window.AuthManager && window.AuthManager.prototype.init;
                if (originalInit) {
                    window.AuthManager.prototype.init = async function() {
                        await originalInit.call(this);
                        if (window.mobileMenu) {
                            window.mobileMenu.updateAuthSection();
                        }
                    };
                }
            }
        }, 100);

        // Timeout después de 5 segundos
        setTimeout(() => {
            clearInterval(checkAuth);
            if (!window.auth) {
                this.updateAuthSection(); // Mostrar botones de login
            }
        }, 5000);
    }

    toggle() {
        if (this.isOpen) {
            this.close();
        } else {
            this.open();
        }
    }

    open() {
        this.isOpen = true;
        this.menuToggle.classList.add('active');
        this.mobileMenu.classList.add('active');
        document.body.classList.add('menu-open');
        this.updateAuthSection(); // Actualizar contenido de auth al abrir
    }

    close() {
        this.isOpen = false;
        this.menuToggle.classList.remove('active');
        this.mobileMenu.classList.remove('active');
        document.body.classList.remove('menu-open');
    }
}

// Inicializar el gestor del menú móvil
window.mobileMenu = new MobileMenuManager();

// Función global para cerrar sesión (usada en el menú móvil)
if (typeof window.cerrarSesion === 'undefined') {
    window.cerrarSesion = async function() {
        if (window.notify && window.notify.confirm) {
            window.notify.confirm(
                '¿Cerrar Sesión?',
                '¿Estás seguro de que deseas cerrar sesión?',
                async () => {
                    try {
                        const response = await fetch('/LogoutServlet', {
                            method: 'POST'
                        });
                        
                        if (response.ok) {
                            window.location.href = '../index.html?logout=exitoso';
                        } else {
                            if (window.notify) {
                                window.notify.error('Error', 'No se pudo cerrar sesión');
                            }
                        }
                    } catch (error) {
                        console.error('Error al cerrar sesión:', error);
                        if (window.notify) {
                            window.notify.error('Error', 'Ocurrió un error al cerrar sesión');
                        }
                    }
                }
            );
        } else {
            // Fallback si notify no está disponible
            if (confirm('¿Estás seguro de que deseas cerrar sesión?')) {
                try {
                    const response = await fetch('/LogoutServlet', {
                        method: 'POST'
                    });
                    
                    if (response.ok) {
                        window.location.href = '../index.html?logout=exitoso';
                    } else {
                        alert('No se pudo cerrar sesión');
                    }
                } catch (error) {
                    console.error('Error al cerrar sesión:', error);
                    alert('Ocurrió un error al cerrar sesión');
                }
            }
        }
    };
}