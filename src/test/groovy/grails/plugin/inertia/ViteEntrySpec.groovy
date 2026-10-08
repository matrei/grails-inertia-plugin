package grails.plugin.inertia

import spock.lang.Specification

class ViteEntrySpec extends Specification {

    void 'the stylesheets and modules of the chunks imported by the entry are included'() {

        given: 'an entry importing a shared chunk with css, and a vendor chunk that also imports the shared chunk'
            final manifest = [
                    'src/main/javascript/main.js': [
                            file: 'js/main.js',
                            isEntry: true,
                            imports: ['_shared.js', '_vendor.js'],
                            css: ['js/main.css']
                    ],
                    '_shared.js': [file: 'js/shared.js', css: ['js/shared.css']],
                    '_vendor.js': [file: 'js/vendor.js', imports: ['_shared.js']]
            ] as Map<String, Map<String, Object>>

        when: 'resolving the entry'
            final entry = ViteEntry.from(manifest, 'src/main/javascript/main.js')

        then: 'the entry file, all stylesheets, and each imported chunk once'
            entry.file == 'js/main.js'
            entry.css == ['js/main.css', 'js/shared.css']
            entry.preloads == ['js/shared.js', 'js/vendor.js']
    }

    void 'imported chunks are listed after the chunks they import'() {

        given: 'a chain of imports'
            final manifest = [
                    'main.js': [file: 'main.js', imports: ['_a.js']],
                    '_a.js': [file: 'a.js', imports: ['_b.js'], css: ['a.css']],
                    '_b.js': [file: 'b.js', imports: ['_c.js'], css: ['b.css']],
                    '_c.js': [file: 'c.js', css: ['c.css']]
            ] as Map<String, Map<String, Object>>

        when: 'resolving the entry'
            final entry = ViteEntry.from(manifest, 'main.js')

        then: 'dependencies come first'
            entry.css == ['c.css', 'b.css', 'a.css']
            entry.preloads == ['c.js', 'b.js', 'a.js']
    }

    void 'an entry without imports or css has nothing more to load'() {

        when: 'resolving an entry with only a file'
            final entry = ViteEntry.from(['main.js': [file: 'main.js']] as Map<String, Map<String, Object>>, 'main.js')

        then: 'only the entry file'
            entry.file == 'main.js'
            entry.css.empty
            entry.preloads.empty
    }

    void 'an entry missing from the manifest is reported'() {

        when: 'resolving an unknown entry'
            ViteEntry.from([:], 'src/main/javascript/main.js')

        then: 'the error names the entry'
            final e = thrown(IllegalArgumentException)
            e.message.contains('src/main/javascript/main.js')
    }
}
