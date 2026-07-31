/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./index.html",
    "./js/**/*.js"
  ],
  theme: {
    extend: {
      colors: {
        sage: '#5C6757',
        sageLight: '#E4E8E3',
        sageDark: '#3F473B',
        emerald: '#3B7A57',
        coral: '#D87D56',
        warmBg: '#F4F3EF',
        surface: '#FFFFFF',
        surfaceCard: '#FAF9F6',
        primaryText: '#2D332A',
        mutedText: '#7C8079',
        borderDef: '#E5E3DC'
      },
      fontFamily: {
        headers: ['Fraunces', 'serif'],
        body: ['DM Sans', 'sans-serif']
      }
    }
  },
  plugins: [],
}
